package com.beathub.multiarenas.delivery.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.sso.azure")
public class AzureSsoProperties {

    /**
     * Habilita o deshabilita la validación de tokens de Azure SSO.
     */
    private boolean enabled = true;

    /**
     * Endpoint JWKS (JSON Web Key Set) de Azure AD / B2C para descargar las claves públicas RSA.
     */
    private String jwksUrl = "https://ssopreprod.b2clogin.com/ssopreprod.onmicrosoft.com/b2c_1a_passwordless_signin-optpass/discovery/v2.0/keys";

    /**
     * Endpoint OpenID Connect well-known configuration.
     */
    private String wellKnownUrl = "https://ssopreprod.b2clogin.com/ssopreprod.onmicrosoft.com/B2C_1A_PASSWORDLESS_SIGNIN-OPTPASS/v2.0/.well-known/openid-configuration";

    /**
     * Client ID principal esperado de la aplicación registrada en Azure AD / B2C.
     */
    private String clientId = "63f8ef93-042a-40eb-a2f0-ba7b574e5ea6";

    /**
     * Lista de Client IDs aceptados para soportar diferentes canales de cliente (USER_APP, RUNNER_APP, ADMIN_WEB).
     */
    private List<String> acceptedClientIds = new ArrayList<>(List.of("63f8ef93-042a-40eb-a2f0-ba7b574e5ea6"));

    /**
     * Emisor (Issuer) esperado en los claims del token.
     */
    private String issuer = "https://ssopreprod.b2clogin.com/bea47667-153a-452f-9723-8e8b4071f126/v2.0/";

    /**
     * Indica si se valida criptográficamente la firma del token con las claves públicas de Azure.
     */
    private boolean validateSignature = true;

    /**
     * Indica si se valida el claim de audiencia (aud).
     */
    private boolean validateAudience = true;

    /**
     * Indica si se valida estrictamente el claim del emisor (iss).
     */
    private boolean validateIssuer = false;

    /**
     * Tolerancia de desincronización de reloj en segundos (Clock Skew).
     */
    private long clockSkewSeconds = 120L;

    /**
     * Tiempo de vida en minutos de la caché en memoria de claves públicas JWKS.
     */
    private long cacheTtlMinutes = 60L;
}
