package com.beathub.multiarenas.delivery.auth.service.business.impl;

import com.beathub.multiarenas.delivery.auth.dto.response.PagedResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import com.beathub.multiarenas.delivery.auth.entity.Usuario;
import com.beathub.multiarenas.delivery.auth.exception.ForbiddenException;
import com.beathub.multiarenas.delivery.auth.exception.ResourceNotFoundException;
import com.beathub.multiarenas.delivery.auth.repository.UsuarioRepository;
import com.beathub.multiarenas.delivery.auth.repository.specification.UsuarioSpecification;
import com.beathub.multiarenas.delivery.auth.security.SecurityUtils;
import com.beathub.multiarenas.delivery.auth.service.business.AuthService;
import com.beathub.multiarenas.delivery.auth.service.business.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse obtenerPorId(Long usuarioId) {
        return authService.obtenerPerfil(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse obtenerPorUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        return authService.obtenerPerfil(usuario.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserProfileResponse> listarUsuarios(Long arenaId, Integer estadoId, String rol, String search, Pageable pageable) {
        Long arenaFiltro = arenaId;
        boolean esSuperAdminOBackoffice = SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.hasRole("BACKOFFICE");

        if (!esSuperAdminOBackoffice) {
            if (arenaFiltro != null) {
                if (!SecurityUtils.hasArenaAccess(arenaFiltro)) {
                    throw new ForbiddenException("No tiene permisos para consultar usuarios de la arena " + arenaFiltro);
                }
            } else {
                arenaFiltro = SecurityUtils.getCurrentArenaId().orElse(null);
            }
        }

        Specification<Usuario> spec = UsuarioSpecification.conFiltros(arenaFiltro, estadoId, rol, search);
        Page<Usuario> paginaUsuarios = usuarioRepository.findAll(spec, pageable);

        Page<UserProfileResponse> paginaDto = paginaUsuarios.map(authService::mapearUsuarioAProfileResponse);

        return PagedResponse.from(paginaDto);
    }
}
