package com.beathub.multiarenas.delivery.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsoLoginRequest {

    @NotBlank(message = "El identificador de SSO (ssoId) es obligatorio")
    @JsonAlias({"ssoid", "ssoId"})
    private String ssoId;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de email es inválido")
    private String email;

    @JsonAlias({"clienttype", "clientType"})
    @Builder.Default
    private ClientType clientType = ClientType.USER_APP;

    @JsonAlias({"arena", "arenaId"})
    private Long arenaId;

    @NotBlank(message = "El token de SSO (ssoToken) es obligatorio")
    @JsonAlias({"ssotoken", "ssoToken"})
    private String ssoToken;
}
