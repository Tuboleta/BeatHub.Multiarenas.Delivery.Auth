package com.beathub.multiarenas.delivery.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información del Runner disponible para asignación")
public class RunnerDisponibleResponse {

    @Schema(description = "ID del usuario Runner", example = "5")
    private Long usuarioId;

    @Schema(description = "Nombre de usuario", example = "juan.runner")
    private String username;

    @Schema(description = "Nombres completos", example = "Juan Carlos")
    private String nombres;

    @Schema(description = "Apellidos", example = "Rodríguez")
    private String apellidos;

    @Schema(description = "Teléfono móvil para contacto operativo", example = "+573001234567")
    private String telefono;

    @Schema(description = "Prioridad de la zona consultada para este Runner (1 = Principal, 2 = Secundaria, 3 = Terciaria)", example = "1")
    private Integer prioridadZona;

    @Schema(description = "Estado de disponibilidad (true = disponible)", example = "true")
    @Builder.Default
    private Boolean disponible = true;

    @Schema(description = "Lista de todas las zonas asignadas a este Runner")
    @Builder.Default
    private List<RunnerZonaDto> zonasAsignadas = new ArrayList<>();
}

