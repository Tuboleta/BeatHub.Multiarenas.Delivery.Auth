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
    @JsonAlias({ "ssoid", "ssoId" })
    private String ssoId;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de email es inválido")
    private String email;

    @JsonAlias({ "nombres", "givenName", "given_name", "firstName", "first_name" })
    private String nombres;

    @JsonAlias({ "apellidos", "familyName", "family_name", "lastName", "last_name" })
    private String apellidos;

    @JsonAlias({ "clienttype", "clientType" })
    @Builder.Default
    private ClientType clientType = ClientType.USER_APP;

    @JsonAlias({ "arena", "arenaId" })
    private Long arenaId;

    @NotBlank(message = "El token de SSO (ssoToken) es obligatorio")
    @JsonAlias({ "ssotoken", "ssoToken" })
    private String ssoToken;

    @JsonAlias({ "numeroDocumento", "numero_documento", "documento", "identificationNumber", "identification_number" })
    private String numeroDocumento;

    @JsonAlias({ "tipoDocumentoId", "tipo_documento_id", "tipoDocumento", "identificationType" })
    private Integer tipoDocumentoId;

    @JsonAlias({ "direccion", "address", "streetAddress", "street" })
    private String direccion;

    @JsonAlias({ "ciudad", "city" })
    private String ciudad;

    @JsonAlias({ "departamento", "state" })
    private String departamento;

    @JsonAlias({ "codigoPostal", "codigo_postal", "postalCode", "zip" })
    private String codigoPostal;

    @JsonAlias({ "telefono", "phone", "celular", "mobile" })
    private String telefono;
}
