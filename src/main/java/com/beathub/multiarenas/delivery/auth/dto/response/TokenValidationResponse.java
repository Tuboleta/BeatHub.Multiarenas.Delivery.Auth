package com.beathub.multiarenas.delivery.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private Long usuarioId;
    private String username;
    private String email;
    private String ssoId;
    private Long currentArenaId;
    private List<String> roles;
    private List<String> scopes;
    private List<Long> arenaIds;
    private String clientType;
    private Long shopId;
    private String shopNombre;
}
