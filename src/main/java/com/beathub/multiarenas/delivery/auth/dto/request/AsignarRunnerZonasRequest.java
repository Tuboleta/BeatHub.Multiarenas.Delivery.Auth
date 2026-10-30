package com.beathub.multiarenas.delivery.auth.dto.request;

import com.beathub.multiarenas.delivery.auth.dto.response.RunnerZonaDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Solicitud para asignar o actualizar las zonas de cobertura de un Runner")
public class AsignarRunnerZonasRequest {

    @NotNull(message = "La lista de zonas no puede ser nula")
    @Size(max = 3, message = "Un runner no puede tener más de 3 zonas asignadas simultáneamente")
    @Valid
    @Builder.Default
    @Schema(description = "Lista de 1 a 3 zonas asignadas al Runner con su prioridad")
    private List<RunnerZonaDto> zonas = new ArrayList<>();
}

