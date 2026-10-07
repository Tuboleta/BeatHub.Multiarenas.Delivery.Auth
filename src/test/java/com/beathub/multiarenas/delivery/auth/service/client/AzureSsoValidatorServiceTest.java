package com.beathub.multiarenas.delivery.auth.service.client;

import com.beathub.multiarenas.delivery.auth.config.AzureSsoProperties;
import com.beathub.multiarenas.delivery.auth.dto.sso.AzureTokenClaims;
import com.beathub.multiarenas.delivery.auth.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AzureSsoValidatorServiceTest {

    private AzureSsoValidatorService validatorService;
    private AzureSsoProperties properties;
    private RestClient restClient;
    private ObjectMapper objectMapper;

    private KeyPair keyPair;
    private static final String TEST_KID = "test-azure-kid-123";
    private static final String TEST_CLIENT_ID = "63f8ef93-042a-40eb-a2f0-ba7b574e5ea6";
    private static final String TEST_ISSUER = "https://ssopreprod.b2clogin.com/bea47667-153a-452f-9723-8e8b4071f126/v2.0/";

    @BeforeEach
    void setUp() throws Exception {
        properties = new AzureSsoProperties();
        properties.setEnabled(true);
        properties.setValidateSignature(true);
        properties.setValidateAudience(true);
        properties.setValidateIssuer(false);
        properties.setClientId(TEST_CLIENT_ID);
        properties.setAcceptedClientIds(List.of(TEST_CLIENT_ID));
        properties.setIssuer(TEST_ISSUER);
        properties.setClockSkewSeconds(60L);

        restClient = Mockito.mock(RestClient.class);
        objectMapper = new ObjectMapper();

        validatorService = new AzureSsoValidatorService(properties, restClient, objectMapper);

        // Generar par de claves RSA reales para pruebas
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        // Registrar la clave pública en caché del servicio para simular respuesta JWKS
        validatorService.registrarClavePublica(TEST_KID, keyPair.getPublic());
    }

    private String buildValidToken(String sub, String email, String name, Date exp, String aud) {
        return Jwts.builder()
                .header().keyId(TEST_KID).and()
                .subject(sub)
                .claim("emails", List.of(email))
                .claim("name", name)
                .claim("given_name", "Juan")
                .claim("family_name", "Pérez")
                .audience().add(aud != null ? aud : TEST_CLIENT_ID).and()
                .issuer(TEST_ISSUER)
                .issuedAt(new Date())
                .expiration(exp != null ? exp : new Date(System.currentTimeMillis() + 3600000L))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    @Test
    void validarToken_TokenNuloOVacio_LanzaUnauthorizedException() {
        assertThrows(UnauthorizedException.class, () ->
                validatorService.validarToken(null, "sub-123", "juan@test.com")
        );
        assertThrows(UnauthorizedException.class, () ->
                validatorService.validarToken("   ", "sub-123", "juan@test.com")
        );
    }

    @Test
    void validarToken_TokenFormatoInvalido_LanzaUnauthorizedException() {
        assertThrows(UnauthorizedException.class, () ->
                validatorService.validarToken("formato.invalido", "sub-123", "juan@test.com")
        );
    }

    @Test
    void validarToken_ExitosoConFirmaRsaYClaimsValidos() {
        String sub = "azure-user-guid-001";
        String email = "juan.perez@tuboleta.com";
        String name = "Juan Pérez";

        String token = buildValidToken(sub, email, name, null, null);

        AzureTokenClaims claims = validatorService.validarToken(token, sub, email);

        assertNotNull(claims);
        assertEquals(sub, claims.getSubject());
        assertEquals(email, claims.getEmail());
        assertEquals(name, claims.getName());
        assertEquals("Juan", claims.getGivenName());
        assertEquals("Pérez", claims.getFamilyName());
        assertTrue(claims.getAudience().contains(TEST_CLIENT_ID));
    }

    @Test
    void validarToken_TokenExpirado_LanzaUnauthorizedException() {
        String sub = "azure-user-guid-001";
        String email = "juan.perez@tuboleta.com";
        Date fechaPasada = new Date(System.currentTimeMillis() - 7200000L); // 2 horas atrás

        String token = buildValidToken(sub, email, "Juan", fechaPasada, null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                validatorService.validarToken(token, sub, email)
        );
        assertTrue(ex.getMessage().contains("expirado"));
    }

    @Test
    void validarToken_FirmaInvalida_LanzaUnauthorizedException() throws Exception {
        // Generar otra clave privada diferente
        KeyPairGenerator anotherGen = KeyPairGenerator.getInstance("RSA");
        anotherGen.initialize(2048);
        KeyPair anotherKeyPair = anotherGen.generateKeyPair();

        String tokenFirmadoConOtraClave = Jwts.builder()
                .header().keyId(TEST_KID).and()
                .subject("azure-user-guid-001")
                .audience().add(TEST_CLIENT_ID).and()
                .issuer(TEST_ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(anotherKeyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                validatorService.validarToken(tokenFirmadoConOtraClave, "azure-user-guid-001", null)
        );
        assertTrue(ex.getMessage().contains("inválida"));
    }

    @Test
    void validarToken_DiscrepanciaSsoId_AntiSpoofing_LanzaUnauthorizedException() {
        String subReal = "azure-real-guid";
        String ssoIdAtacante = "azure-impersonated-guid";
        String email = "victim@tuboleta.com";

        String token = buildValidToken(subReal, email, "Victima", null, null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                validatorService.validarToken(token, ssoIdAtacante, email)
        );
        assertTrue(ex.getMessage().contains("no coincide con el sujeto verificado"));
    }

    @Test
    void validarToken_DiscrepanciaEmail_LanzaUnauthorizedException() {
        String sub = "azure-real-guid";
        String emailRealToken = "real@tuboleta.com";
        String emailSolicitud = "fake@tuboleta.com";

        String token = buildValidToken(sub, emailRealToken, "Usuario", null, null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                validatorService.validarToken(token, sub, emailSolicitud)
        );
        assertTrue(ex.getMessage().contains("no coincide con el correo verificado"));
    }

    @Test
    void validarToken_AudienciaInvalida_LanzaUnauthorizedException() {
        String sub = "azure-real-guid";
        String email = "user@tuboleta.com";
        String audInvalida = "client-id-no-autorizado";

        String token = buildValidToken(sub, email, "Usuario", null, audInvalida);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                validatorService.validarToken(token, sub, email)
        );
        assertTrue(ex.getMessage().contains("audiencia"));
    }
}
