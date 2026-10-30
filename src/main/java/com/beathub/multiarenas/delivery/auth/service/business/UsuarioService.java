package com.beathub.multiarenas.delivery.auth.service.business;

import com.beathub.multiarenas.delivery.auth.dto.request.ActualizarUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.CrearUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.PagedResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    UserProfileResponse crearUsuario(CrearUsuarioRequest request);

    UserProfileResponse actualizarUsuario(Long usuarioId, ActualizarUsuarioRequest request);

    void cambiarEstado(Long usuarioId, Integer nuevoEstadoId);

    UserProfileResponse obtenerPorId(Long usuarioId);

    UserProfileResponse obtenerPorUsername(String username);

    PagedResponse<UserProfileResponse> listarUsuarios(Long arenaId, Integer estadoId, String rol, String search, Pageable pageable);
}
