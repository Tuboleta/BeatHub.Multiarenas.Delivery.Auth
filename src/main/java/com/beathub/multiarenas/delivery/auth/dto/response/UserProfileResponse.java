package com.beathub.multiarenas.delivery.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long usuarioId;
    private String username;
    private String ssoId;
    private String ssoProvider;
    private String tcposClientId;
    private Long shopId;
    private String shopNombre;
    private Integer estadoId;
    private Long personaId;
    private String nombres;
    private String apellidos;
    private Integer tipoDocumentoId;
    private String numeroDocumento;
    private String email;
    private String telefono;
    private Integer paisId;
    private Integer departamentoId;
    private Integer ciudadId;
    private String direccion;
    private List<String> roles;
    private List<String> scopes;
    private List<Long> arenaIds;
    private LocalDateTime creacionFecha;
}
