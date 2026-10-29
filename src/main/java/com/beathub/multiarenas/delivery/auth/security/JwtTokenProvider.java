package com.beathub.multiarenas.delivery.auth.security;

import com.beathub.multiarenas.delivery.auth.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(Long usuarioId, String username, String email, String ssoId, Long personaId, List<String> roles, List<String> scopes, Long currentArenaId, List<Long> arenaIds, String clientType, Long shopId, String shopNombre) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpirationMs());

        var builder = Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("usuarioId", usuarioId)
                .claim("id", usuarioId)
                .claim("username", username)
                .claim("email", email)
                .claim("personaId", personaId)
                .claim("roles", roles)
                .claim("scopes", scopes)
                .claim("arenaIds", arenaIds);

        if (clientType != null && !clientType.isBlank()) {
            builder.claim("clientType", clientType);
        }

        if (ssoId != null && !ssoId.isBlank()) {
            builder.claim("ssoId", ssoId);
        }

        if (currentArenaId != null) {
            builder.claim("currentArenaId", currentArenaId);
        }

        if (shopId != null) {
            builder.claim("shopId", shopId);
        }

        if (shopNombre != null && !shopNombre.isBlank()) {
            builder.claim("shopNombre", shopNombre);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    public String generarToken(Long usuarioId, String username, String email, String ssoId, Long personaId, List<String> roles, List<String> scopes, Long currentArenaId, List<Long> arenaIds, String clientType) {
        return generarToken(usuarioId, username, email, ssoId, personaId, roles, scopes, currentArenaId, arenaIds, clientType, null, null);
    }

    public String generarToken(Long usuarioId, String username, String email, String ssoId, Long personaId, List<String> roles, List<String> scopes, Long currentArenaId, List<Long> arenaIds) {
        return generarToken(usuarioId, username, email, ssoId, personaId, roles, scopes, currentArenaId, arenaIds, null, null, null);
    }

    public String generarRefreshToken(Long usuarioId, String username, Long currentArenaId, String clientType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshExpirationMs());

        var builder = Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("usuarioId", usuarioId)
                .claim("username", username)
                .claim("tokenType", "REFRESH");

        if (clientType != null && !clientType.isBlank()) {
            builder.claim("clientType", clientType);
        }

        if (currentArenaId != null) {
            builder.claim("currentArenaId", currentArenaId);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    public String generarRefreshToken(Long usuarioId, String username, Long currentArenaId) {
        return generarRefreshToken(usuarioId, username, currentArenaId, null);
    }

    public String generarToken(UserPrincipal userPrincipal) {
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList();

        return generarToken(
                userPrincipal.getUsuarioId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getSsoId(),
                userPrincipal.getPersonaId(),
                roles,
                userPrincipal.getScopes(),
                userPrincipal.getCurrentArenaId(),
                userPrincipal.getArenaIds(),
                userPrincipal.getClientType(),
                userPrincipal.getShopId(),
                userPrincipal.getShopNombre()
        );
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validarToken(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Firma JWT no válida: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Token JWT malformado: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Token JWT expirado: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT no soportado: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("Claims JWT vacíos o nulos: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Error al validar token JWT: {}", ex.getMessage());
        }
        return false;
    }

    public boolean validarRefreshToken(String token) {
        if (!validarToken(token)) {
            return false;
        }
        try {
            Claims claims = extraerClaims(token);
            String tokenType = claims.get("tokenType", String.class);
            return "REFRESH".equalsIgnoreCase(tokenType);
        } catch (Exception ex) {
            log.error("Error al validar refresh token JWT: {}", ex.getMessage());
            return false;
        }
    }

    public Long extraerUsuarioId(String token) {
        return Long.parseLong(extraerClaims(token).getSubject());
    }

    public UserPrincipal extraerUserPrincipal(String token) {
        Claims claims = extraerClaims(token);
        return UserPrincipal.create(claims);
    }
}
