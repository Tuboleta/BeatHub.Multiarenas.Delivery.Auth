package com.beathub.multiarenas.delivery.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para la creación y registro de usuario desde el BackOffice")
public class CrearUsuarioRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    @Schema(description = "Nombres de la persona", example = "Juan Carlos", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Schema(description = "Apellidos de la persona", example = "Rodríguez Silva", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apellidos;

    @NotNull(message = "El tipo de documento es obligatorio")
    @Schema(description = "ID del tipo de documento (1 = Cédula de Ciudadanía, 2 = Cédula de Extranjería, 3 = Pasaporte)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer tipoDocumentoId;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 50, message = "El número de documento no puede exceder 50 caracteres")
    @Schema(description = "Número del documento de identidad", example = "1020304050", requiredMode = Schema.RequiredMode.REQUIRED)
    private String numeroDocumento;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato de correo electrónico es inválido")
    @Size(max = 150, message = "El email no puede exceder 150 caracteres")
    @Schema(description = "Correo electrónico único del usuario", example = "juan.rodriguez@daviarrena.com", requiredMode = Schema.RequiredMode.REQUIRED)
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
    @Schema(description = "Nombre de usuario único (opcional; si se omite, se genera a partir del email)", example = "juan.rodriguez")
    private String username;

    @Size(min = 6, message = "La contraseña debe tener un mínimo de 6 caracteres")
    @Schema(description = "Contraseña para login directo con credenciales (opcional; si se omite, queda habilitado para autenticación vía SSO)", example = "BeatHub2026*")
    private String password;

    @Schema(description = "Identificador de la arena física a vincular (ej. 1 = DaviArena, 2 = MovistarArena)", example = "1")
    private Long arenaId;

    @Schema(description = "ID del rol a asignar (1 = SUPER_ADMIN, 2 = ADMIN_ARENA, 3 = RUNNER, 4 = CLIENTE, 5 = BACKOFFICE, 6 = BARRA)", example = "3")
    private Long rolId;

    @Schema(description = "Nombre textual del rol alternativo si no se especifica rolId (ej. 'RUNNER', 'BARRA', 'ADMIN_ARENA', 'BACKOFFICE', 'CLIENTE')", example = "RUNNER")
    private String rolNombre;

    @Schema(description = "Identificador del punto de venta o barra física asignada si aplica", example = "101")
    private Long shopId;

    @Schema(description = "Nombre comercial de la barra o punto de venta físico", example = "Barra Sur - Coctelería")
    private String shopNombre;

    @Schema(description = "Proveedor de identidad SSO si aplica (por defecto BACKOFFICE si tiene password o TUBOLETA)", example = "BACKOFFICE")
    private String ssoProvider;

    @Schema(description = "Identificador único en el proveedor SSO si aplica", example = "sub_azure_98765")
    private String ssoId;

    @Schema(description = "Identificador de cliente en sistema TCPOS WOND si aplica", example = "CLI-TCPOS-101")
    private String tcposClientId;

    @Schema(description = "Estado inicial del usuario (1 = Activo, 2 = Inactivo, por defecto 1)", example = "1")
    private Integer estadoId;
}

