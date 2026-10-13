package com.beathub.multiarenas.delivery.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Petición de renovación de token JWT mediante Refresh Token (segundo plano)")
public class RefreshTokenRequest {

    @NotBlank(message = "El refresh token es obligatorio")
    @Schema(description = "Refresh token obtenido previamente", example = "eyJhbGciOiJIUzI1NiJ9...")
    @JsonAlias({"refresh_token", "token"})
    private String refreshToken;

    @Schema(description = "ID opcional de la nueva arena a la que se desea asociar el token", example = "1")
    @JsonAlias({"arena", "arena_id"})
    private Long arenaId;
}
