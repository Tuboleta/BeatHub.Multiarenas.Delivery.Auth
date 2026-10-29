package com.beathub.multiarenas.delivery.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para asignar o rotar el punto de venta (Shop) de un usuario en una arena")
public class ActualizarShopUsuarioRequest {

    @Schema(description = "Identificador del Shop / Tienda en TCPOS y Catálogo (null para desvincular)", example = "101")
    private Long shopId;

    @Schema(description = "Nombre legible de la barra o local asignado", example = "Barra Norte Nivel 1")
    private String shopNombre;
}

