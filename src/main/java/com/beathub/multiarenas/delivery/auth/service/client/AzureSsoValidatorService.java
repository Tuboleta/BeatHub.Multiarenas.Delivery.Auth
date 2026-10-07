package com.beathub.multiarenas.delivery.auth.service.client;

import com.beathub.multiarenas.delivery.auth.config.AzureSsoProperties;
import com.beathub.multiarenas.delivery.auth.dto.sso.AzureTokenClaims;
import com.beathub.multiarenas.delivery.auth.exception.UnauthorizedException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class AzureSsoValidatorService {

    private final AzureSsoProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AzureSsoValidatorService(AzureSsoProperties properties,
                                    RestClient restClient,
                                    @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private final AtomicLong lastFetchTime = new AtomicLong(0L);

    /**
     * Valida de extremo a extremo el token emitido por Azure (AD / B2C / Entra ID),
     * comprobando formato, firma criptográfica RSA contra Azure JWKS, vigencia temporal,
     * audiencia y correspondencia estricta con el ssoId y email solicitados.
     *
     * @param token         Token JWT provisto por el proveedor SSO de Azure.
     * @param expectedSsoId Identificador ssoId esperado (sub / oid).
     * @param expectedEmail Correo electrónico esperado del usuario.
     * @return AzureTokenClaims con los datos validados y verificados.
     */
    public AzureTokenClaims validarToken(String token, String expectedSsoId, String expectedEmail) {
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("El token de autenticación SSO (ssoToken) es obligatorio");
        }

        String trimmedToken = token.trim();
        String[] parts = trimmedToken.split("\\.");
        if (parts.length != 3) {
            throw new UnauthorizedException("El token de SSO de Azure no tiene un formato JWT válido");
        }

        // 1. Decodificar encabezado para verificar algoritmo y kid
        JsonNode headerNode;
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            headerNode = objectMapper.readTree(new String(headerBytes, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("Error al decodificar el encabezado del token de Azure: {}", ex.getMessage());
            throw new UnauthorizedException("El encabezado del token de SSO de Azure es inválido");
        }

        String kid = headerNode.hasNonNull("kid") ? headerNode.get("kid").asText() : null;
        String alg = headerNode.hasNonNull("alg") ? headerNode.get("alg").asText() : null;

        if (alg != null && !"RS256".equalsIgnoreCase(alg) && !"RSA".equalsIgnoreCase(alg)) {
            throw new UnauthorizedException("Algoritmo de firma de token no permitido: " + alg + ". Se requiere RS256.");
        }

        Claims claims;

        // 2. Validación de firma criptográfica y expiración
        if (properties.isEnabled() && properties.isValidateSignature()) {
            PublicKey publicKey = resolvePublicKey(kid);
            try {
                claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .clockSkewSeconds(properties.getClockSkewSeconds())
                        .build()
                        .parseSignedClaims(trimmedToken)
                        .getPayload();
            } catch (ExpiredJwtException ex) {
                log.warn("Token de Azure SSO expirado: {}", ex.getMessage());
                throw new UnauthorizedException("El token de SSO de Azure ha expirado");
            } catch (SignatureException ex) {
                log.error("Firma criptográfica de Azure SSO inválida: {}", ex.getMessage());
                throw new UnauthorizedException("La firma criptográfica del token de SSO de Azure es inválida");
            } catch (MalformedJwtException ex) {
                log.error("Token de Azure SSO malformado: {}", ex.getMessage());
                throw new UnauthorizedException("El formato del token de SSO de Azure es inválido");
            } catch (JwtException ex) {
                log.error("Fallo general al validar token de Azure SSO: {}", ex.getMessage());
                throw new UnauthorizedException("El token de SSO de Azure es inválido: " + ex.getMessage());
            }
        } else {
            // Modo mock / desarrollo sin validación criptográfica remota
            try {
                byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
                Map<String, Object> map = objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});
                claims = Jwts.claims().add(map).build();

                Date exp = claims.getExpiration();
                if (exp != null) {
                    long nowWithSkew = System.currentTimeMillis() - (properties.getClockSkewSeconds() * 1000L);
                    if (exp.getTime() < nowWithSkew) {
                        throw new UnauthorizedException("El token de SSO de Azure ha expirado");
                    }
                }
            } catch (UnauthorizedException ue) {
                throw ue;
            } catch (Exception ex) {
                log.error("Error al procesar payload del token de Azure SSO sin firma: {}", ex.getMessage());
                throw new UnauthorizedException("El cuerpo del token de SSO de Azure es inválido");
            }
        }

        // 3. Extracción de claims relevantes
        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject) && claims.get("oid") != null) {
            subject = claims.get("oid").toString();
        }
        if (!StringUtils.hasText(subject)) {
            throw new UnauthorizedException("El token de SSO de Azure no contiene un identificador de sujeto ('sub' u 'oid')");
        }

        String email = extractEmailFromClaims(claims);
        String name = claims.get("name", String.class);
        String givenName = claims.get("given_name", String.class);
        String familyName = claims.get("family_name", String.class);
        String issuer = claims.getIssuer();
        Set<String> audience = claims.getAudience();
        Date expiration = claims.getExpiration();

        // 4. Validación de Audiencia (aud)
        if (properties.isValidateAudience()) {
            List<String> accepted = properties.getAcceptedClientIds();
            boolean matched = false;
            if (audience != null && !audience.isEmpty()) {
                for (String aud : audience) {
                    if ((accepted != null && accepted.contains(aud)) ||
                            (properties.getClientId() != null && properties.getClientId().equalsIgnoreCase(aud))) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                log.warn("Audiencia del token de Azure SSO ({}) no coincide con los Client IDs configurados", audience);
                throw new UnauthorizedException("La audiencia del token de Azure SSO no coincide con los clientes autorizados");
            }
        }

        // 5. Validación de Emisor (iss)
        if (properties.isValidateIssuer() && StringUtils.hasText(properties.getIssuer())) {
            if (issuer == null || !issuer.toLowerCase().startsWith(properties.getIssuer().trim().toLowerCase())) {
                log.warn("Emisor del token de Azure SSO ({}) no coincide con el esperado ({})", issuer, properties.getIssuer());
                throw new UnauthorizedException("El emisor del token de Azure SSO no coincide con el emisor configurado");
            }
        }

        // 6. Validación cruzada de identidad (Anti-Spoofing: ssoId)
        if (StringUtils.hasText(expectedSsoId)) {
            if (!subject.equalsIgnoreCase(expectedSsoId.trim())) {
                log.error("Intento de discrepancia de identidad SSO: ssoId esperado='{}', sujeto token='{}'", expectedSsoId, subject);
                throw new UnauthorizedException("El identificador ssoId proporcionado no coincide con el sujeto verificado del token de Azure");
            }
        }

        // 7. Validación cruzada de correo electrónico si está presente en el token
        if (StringUtils.hasText(expectedEmail) && StringUtils.hasText(email)) {
            if (!email.equalsIgnoreCase(expectedEmail.trim())) {
                log.error("Intento de discrepancia de correo SSO: email esperado='{}', email token='{}'", expectedEmail, email);
                throw new UnauthorizedException("El correo electrónico proporcionado no coincide con el correo verificado del token de Azure");
            }
        }

        return AzureTokenClaims.builder()
                .subject(subject)
                .email(email)
                .name(name)
                .givenName(givenName)
                .familyName(familyName)
                .issuer(issuer)
                .audience(audience)
                .expiration(expiration)
                .rawClaims(claims)
                .build();
    }

    /**
     * Resuelve la clave pública RSA asociada al kid del token de Azure.
     */
    public PublicKey resolvePublicKey(String kid) {
        long cacheTtlMs = properties.getCacheTtlMinutes() * 60 * 1000L;
        boolean expired = (System.currentTimeMillis() - lastFetchTime.get()) > cacheTtlMs;

        if (expired || keyCache.isEmpty() || (kid != null && !keyCache.containsKey(kid))) {
            refreshJwksCache();
        }

        if (kid == null) {
            if (keyCache.size() == 1) {
                return keyCache.values().iterator().next();
            }
            throw new UnauthorizedException("El encabezado del token de Azure no especifica 'kid'");
        }

        PublicKey key = keyCache.get(kid);
        if (key == null) {
            // Forzar recarga si no se encontró en caché (rotación de claves de Azure)
            refreshJwksCache();
            key = keyCache.get(kid);
        }

        if (key == null) {
            throw new UnauthorizedException("No se encontró la clave pública RSA correspondiente a kid '" + kid + "' en Azure");
        }

        return key;
    }

    /**
     * Registra manualmente una clave pública en la caché (útil para pruebas unitarias).
     */
    public void registrarClavePublica(String kid, PublicKey publicKey) {
        this.keyCache.put(kid, publicKey);
        this.lastFetchTime.set(System.currentTimeMillis());
    }

    /**
     * Limpia la caché de claves públicas.
     */
    public void limpiarCache() {
        this.keyCache.clear();
        this.lastFetchTime.set(0L);
    }

    private synchronized void refreshJwksCache() {
        String jwksUrl = properties.getJwksUrl();
        if (!StringUtils.hasText(jwksUrl) && StringUtils.hasText(properties.getWellKnownUrl())) {
            jwksUrl = discoverJwksUrlFromWellKnown(properties.getWellKnownUrl());
        }

        if (!StringUtils.hasText(jwksUrl)) {
            throw new UnauthorizedException("No hay URL de claves públicas JWKS configurada para Azure SSO");
        }

        try {
            String responseBody = restClient.get()
                    .uri(jwksUrl)
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(responseBody)) {
                throw new UnauthorizedException("Respuesta vacía al consultar el endpoint JWKS de Azure");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode keysNode = root.get("keys");

            if (keysNode != null && keysNode.isArray()) {
                for (JsonNode keyNode : keysNode) {
                    String kty = keyNode.hasNonNull("kty") ? keyNode.get("kty").asText() : "";
                    if ("RSA".equalsIgnoreCase(kty)) {
                        String keyId = keyNode.hasNonNull("kid") ? keyNode.get("kid").asText() : null;
                        String n = keyNode.hasNonNull("n") ? keyNode.get("n").asText() : null;
                        String e = keyNode.hasNonNull("e") ? keyNode.get("e").asText() : null;

                        if (keyId != null && n != null && e != null) {
                            PublicKey pubKey = constructRsaPublicKey(n, e);
                            keyCache.put(keyId, pubKey);
                        }
                    }
                }
            }

            lastFetchTime.set(System.currentTimeMillis());
            log.info("Caché de claves públicas JWKS de Azure actualizado. Claves disponibles: {}", keyCache.keySet());
        } catch (UnauthorizedException ue) {
            throw ue;
        } catch (Exception ex) {
            log.error("Error al obtener o parsear claves públicas JWKS de Azure desde {}: {}", jwksUrl, ex.getMessage());
            if (keyCache.isEmpty()) {
                throw new UnauthorizedException("No se pudo conectar con Azure SSO para obtener claves de verificación: " + ex.getMessage());
            }
        }
    }

    private String discoverJwksUrlFromWellKnown(String wellKnownUrl) {
        try {
            String json = restClient.get().uri(wellKnownUrl).retrieve().body(String.class);
            if (StringUtils.hasText(json)) {
                JsonNode node = objectMapper.readTree(json);
                if (node.hasNonNull("jwks_uri")) {
                    return node.get("jwks_uri").asText();
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudo obtener jwks_uri desde well-known configuration {}: {}", wellKnownUrl, ex.getMessage());
        }
        return null;
    }

    private PublicKey constructRsaPublicKey(String nStr, String eStr) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(nStr);
        byte[] eBytes = Base64.getUrlDecoder().decode(eStr);
        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    private String extractEmailFromClaims(Claims claims) {
        Object emailsObj = claims.get("emails");
        if (emailsObj instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            return list.get(0).toString().trim();
        }
        if (emailsObj instanceof String s && StringUtils.hasText(s)) {
            return s.trim();
        }

        Object emailObj = claims.get("email");
        if (emailObj != null && StringUtils.hasText(emailObj.toString())) {
            return emailObj.toString().trim();
        }

        Object upn = claims.get("preferred_username");
        if (upn != null && StringUtils.hasText(upn.toString())) {
            return upn.toString().trim();
        }

        Object uniqueName = claims.get("unique_name");
        if (uniqueName != null && StringUtils.hasText(uniqueName.toString())) {
            return uniqueName.toString().trim();
        }

        return null;
    }
}
