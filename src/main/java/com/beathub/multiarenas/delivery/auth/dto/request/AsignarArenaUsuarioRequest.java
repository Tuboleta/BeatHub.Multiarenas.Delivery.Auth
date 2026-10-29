package com.beathub.multiarenas.delivery.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignarArenaUsuarioRequest {

    @NotBlank(message = "El email del usuario es obligatorio")
    @Email(message = "El formato de email es inválido")
    private String email;

    @NotNull(message = "El identificador de la arena es obligatorio")
    private Long arenaId;

    @NotNull(message = "El identificador del rol (rolId) es obligatorio")
    private Long rolId; // 1 = SUPER_ADMIN, 2 = ADMIN_ARENA, 3 = RUNNER, 4 = CLIENTE, 5 = BACKOFFICE, 6 = BARRA

    private Long shopId;

    private String shopNombre;

    private String nombres;

    private String apellidos;

    private Integer tipoDocumentoId;

    private String numeroDocumento;

    private String telefono;
}
