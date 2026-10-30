package com.beathub.multiarenas.delivery.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para actualización de datos, credenciales y asignaciones de usuario desde BackOffice")
public class ActualizarUsuarioRequest {

    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    @Schema(description = "Nombres de la persona", example = "Juan Carlos")
    private String nombres;

    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Schema(description = "Apellidos de la persona", example = "Rodríguez Silva")
    private String apellidos;

    @Schema(description = "ID del tipo de documento (1 = Cédula de Ciudadanía, 2 = Cédula de Extranjería, 3 = Pasaporte)", example = "1")
    private Integer tipoDocumentoId;

    @Size(max = 50, message = "El número de documento no puede exceder 50 caracteres")
    @Schema(description = "Número del documento de identidad", example = "1020304050")
    private String numeroDocumento;

    @Email(message = "El formato de correo electrónico es inválido")
    @Size(max = 150, message = "El email no puede exceder 150 caracteres")
    @Schema(description = "Correo electrónico único del usuario", example = "juan.rodriguez@daviarrena.com")
    private String email;

    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
    @Schema(description = "Teléfono de contacto", example = "+573001234567")
    private String telefono;

    @Schema(description = "ID del país (ej. 1 = Colombia)", example = "1")
    private Integer paisId;

    @Schema(description = "ID del departamento o estado (ej. 11 = Bogotá D.C.)", example = "11")
    private Integer departamentoId;

    @Schema(description = "ID de la ciudad (ej. 11001 = Bogotá)", example = "11001")
    private Integer ciudadId;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    @Schema(description = "Dirección de residencia", example = "Calle 63 # 24-00")
    private String direccion;

    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    @Schema(description = "Nombre de usuario único", example = "juan.rodriguez")
    private String username;

    @Size(min = 6, message = "La contraseña debe tener un mínimo de 6 caracteres")
    @Schema(description = "Nueva contraseña si se desea actualizar", example = "NuevaPassword2026*")
    private String password;

    @Schema(description = "Estado del usuario (1 = Activo, 2 = Inactivo)", example = "1")
    private Integer estadoId;

    @Schema(description = "Identificador de la arena física para actualizar asignación", example = "1")
    private Long arenaId;

    @Schema(description = "ID del rol a asignar o actualizar (1 = SUPER_ADMIN, 2 = ADMIN_ARENA, 3 = RUNNER, 4 = CLIENTE, 5 = BACKOFFICE, 6 = BARRA)", example = "6")
    private Long rolId;

    @Schema(description = "Nombre textual del rol alternativo si no se especifica rolId (ej. 'BARRA', 'RUNNER')", example = "BARRA")
    private String rolNombre;

    @Schema(description = "Identificador del punto de venta o barra física asignada si aplica", example = "101")
    private Long shopId;

    @Schema(description = "Nombre comercial de la barra física o mostrador", example = "Barra Sur - Coctelería")
    private String shopNombre;

    @Schema(description = "Indica si el nuevo rol operativo (RUNNER o BARRA) debe reemplazar al anterior en la arena", example = "true")
    @Builder.Default
    private Boolean reemplazarRolOperativo = true;

    @Schema(description = "Identificador de cliente en sistema TCPOS WOND si aplica", example = "CLI-TCPOS-101")
    private String tcposClientId;
}

