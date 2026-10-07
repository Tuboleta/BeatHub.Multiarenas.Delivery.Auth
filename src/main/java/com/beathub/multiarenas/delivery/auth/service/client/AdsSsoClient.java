package com.beathub.multiarenas.delivery.auth.service.client;

import com.beathub.multiarenas.delivery.auth.dto.sso.AzureTokenClaims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class AdsSsoClient {

    private final RestClient restClient;
    private final String ssoUrl;
    private final AzureSsoValidatorService azureSsoValidatorService;

    public AdsSsoClient(RestClient restClient,
                        @Value("${services.ads-sso.url:https://sso.tbl.mock}") String ssoUrl,
                        AzureSsoValidatorService azureSsoValidatorService) {
        this.restClient = restClient;
        this.ssoUrl = ssoUrl;
        this.azureSsoValidatorService = azureSsoValidatorService;
    }

    public boolean validarTokenExternoSso(String tokenExterno) {
        try {
            return azureSsoValidatorService.validarToken(tokenExterno, null, null) != null;
        } catch (Exception ex) {
            log.warn("Validación de token externo SSO falló: {}", ex.getMessage());
            return false;
        }
    }

    public AzureTokenClaims validarTokenExternoSso(String tokenExterno, String expectedSsoId, String expectedEmail) {
        return azureSsoValidatorService.validarToken(tokenExterno, expectedSsoId, expectedEmail);
    }
}

