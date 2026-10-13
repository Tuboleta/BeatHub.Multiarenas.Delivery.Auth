package com.beathub.multiarenas.delivery.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Petición de inicio de sesión con credenciales directas (BackOffice)")
public class LoginRequest {

    @NotBlank(message = "El username o email es obligatorio")
    @Schema(description = "Nombre de usuario o correo electrónico", example = "backoffice")
    @JsonAlias({"username", "email", "usuario"})
    private String login;

    @NotBlank(message = "La contraseña es obligatoria")
    @Schema(description = "Contraseña en texto plano", example = "Admin12345!")
    private String password;

    @Schema(description = "ID opcional de la arena en la cual operar", example = "1")
    @JsonAlias({"arena", "arena_id"})
    private Long arenaId;

    @NotNull(message = "El clientType es obligatorio y debe ser 'BACKOFFICE'")
    @Schema(description = "Tipo de cliente (obligatorio: BACKOFFICE)", example = "BACKOFFICE")
    @JsonAlias({"client_type", "clienttype"})
    private ClientType clientType;
}
