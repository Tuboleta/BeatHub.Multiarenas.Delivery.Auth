package com.beathub.multiarenas.delivery.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de zonificación de un Runner")
public class RunnerZonaDto {

    @NotNull(message = "El zonaId es obligatorio")
    @Schema(description = "ID de la zona física de la arena", example = "10")
    private Long zonaId;

    @Min(value = 1, message = "La prioridad mínima es 1 (Zona Principal)")
    @Max(value = 3, message = "La prioridad máxima es 3")
    @Builder.Default
    @Schema(description = "Prioridad de asignación (1 = Principal, 2 = Secundaria, 3 = Terciaria)", example = "1")
    private Integer prioridad = 1;

    @Builder.Default
    @Schema(description = "Indica si la asignación está activa para el turno actual", example = "true")
    private Boolean activo = true;

    @Schema(description = "Nombre descriptivo de la zona física si está disponible", example = "Grada Occidental - Nivel 1")
    private String zonaNombre;
}

