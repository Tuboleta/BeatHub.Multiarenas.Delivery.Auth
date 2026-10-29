package com.beathub.multiarenas.delivery.auth.service.business;

import com.beathub.multiarenas.delivery.auth.dto.request.ActualizarShopUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.AsignarArenaUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.LoginRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.RefreshTokenRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.RegistroRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.SsoLoginRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.ValidateTokenRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.AuthResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.TokenValidationResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import com.beathub.multiarenas.delivery.auth.entity.Usuario;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse registro(RegistroRequest request);

    AuthResponse ssoLogin(SsoLoginRequest request);

    TokenValidationResponse validarToken(ValidateTokenRequest request);

    UserProfileResponse obtenerPerfil(Long usuarioId);

    UserProfileResponse mapearUsuarioAProfileResponse(Usuario usuario);

    AuthResponse refreshToken(Long usuarioId, Long newArenaId);

    AuthResponse refreshTokenConRefreshToken(RefreshTokenRequest request);

    UserProfileResponse asignarArenaUsuario(AsignarArenaUsuarioRequest request);

    UserProfileResponse actualizarShopUsuario(Long usuarioId, Long arenaId, ActualizarShopUsuarioRequest request);
}
