package com.beathub.multiarenas.delivery.auth.service.business;

import com.beathub.multiarenas.delivery.auth.dto.response.PagedResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    UserProfileResponse obtenerPorId(Long usuarioId);

    UserProfileResponse obtenerPorUsername(String username);

    PagedResponse<UserProfileResponse> listarUsuarios(Long arenaId, Integer estadoId, String rol, String search, Pageable pageable);
}
