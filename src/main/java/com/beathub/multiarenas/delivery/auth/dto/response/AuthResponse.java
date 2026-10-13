package com.beathub.multiarenas.delivery.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de autenticación con Token JWT")
public class AuthResponse {

    @Schema(description = "Token de autenticación JWT firmado")
    private String token;

    @Schema(description = "Token de renovación JWT para procesos en segundo plano o actualización de sesión")
    private String refreshToken;

    @Schema(description = "Tipo de token", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Tiempo de expiración en milisegundos", example = "86400000")
    private long expiresIn;
}
