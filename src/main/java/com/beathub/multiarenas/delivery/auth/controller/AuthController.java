package com.beathub.multiarenas.delivery.auth.controller;

import com.beathub.multiarenas.delivery.auth.dto.request.ClientType;
import com.beathub.multiarenas.delivery.auth.dto.request.LoginRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.RefreshTokenRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.SsoLoginRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.ValidateTokenRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.ApiResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.AuthResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.TokenValidationResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import com.beathub.multiarenas.delivery.auth.exception.BadRequestException;
import com.beathub.multiarenas.delivery.auth.exception.UnauthorizedException;
import com.beathub.multiarenas.delivery.auth.security.SecurityUtils;
import com.beathub.multiarenas.delivery.auth.security.UserPrincipal;
import com.beathub.multiarenas.delivery.auth.service.business.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de Login Credenciales / BackOffice, SSO TuBoleta / Azure, Validación y Renovación de Tokens JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Inicio de sesión directo con credenciales (Usuario / Contraseña) para BackOffice")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        if (request.getClientType() != ClientType.BACKOFFICE) {
            throw new BadRequestException("El clientType '" + request.getClientType() + "' no es permitido. La autenticación directa solo admite el contexto 'BACKOFFICE'.");
        }
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Inicio de sesión exitoso", response));
    }

    @PostMapping("/backoffice/login")
    @Operation(summary = "Endpoint específico de autenticación para el entorno BackOffice")
    public ResponseEntity<ApiResponse<AuthResponse>> backofficeLogin(@Valid @RequestBody LoginRequest request) {
        if (request.getClientType() != ClientType.BACKOFFICE) {
            throw new BadRequestException("El clientType '" + request.getClientType() + "' no es permitido en este endpoint. Solo se admite el contexto 'BACKOFFICE'.");
        }
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Autenticación BackOffice exitosa", response));
    }

    @PostMapping("/sso/login")
    @Operation(summary = "Inicio de sesión o auto-registro mediante SSO de TuBoleta / Azure (USER_APP, RUNNER_APP, ADMIN_WEB)")
    public ResponseEntity<ApiResponse<AuthResponse>> ssoLogin(@Valid @RequestBody SsoLoginRequest request) {
        AuthResponse response = authService.ssoLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Autenticación SSO exitosa", response));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validar token JWT e introspección de claims (Gateway / Servicios)")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validate(@Valid @RequestBody ValidateTokenRequest request) {
        TokenValidationResponse response = authService.validarToken(request);
        return ResponseEntity.ok(ApiResponse.success("Validación completada", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener perfil del usuario autenticado", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long usuarioId = resolveUserId(principal, headerUserId);
        UserProfileResponse response = authService.obtenerPerfil(usuarioId);
        return ResponseEntity.ok(ApiResponse.success("Perfil de usuario recuperado", response));
    }

    @PostMapping({"/refresh", "/token/refresh"})
    @Operation(summary = "Renovar Token JWT mediante Refresh Token (en segundo plano / background) o sesión activa Bearer")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest refreshRequest,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "arenaId", required = false) Long arenaId) {

        if (refreshRequest != null && StringUtils.hasText(refreshRequest.getRefreshToken())) {
            AuthResponse response = authService.refreshTokenConRefreshToken(refreshRequest);
            return ResponseEntity.ok(ApiResponse.success("Token renovado exitosamente con refresh token", response));
        }

        Long effectiveArenaId = (refreshRequest != null && refreshRequest.getArenaId() != null)
                ? refreshRequest.getArenaId()
                : arenaId;
        Long usuarioId = resolveUserId(principal, headerUserId);
        AuthResponse response = authService.refreshToken(usuarioId, effectiveArenaId);
        return ResponseEntity.ok(ApiResponse.success("Token renovado exitosamente", response));
    }

    private Long resolveUserId(UserPrincipal principal, Long headerUserId) {
        if (principal != null && principal.getUsuarioId() != null) {
            return principal.getUsuarioId();
        }
        if (headerUserId != null) {
            return headerUserId;
        }
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Usuario no autenticado o token expirado. Proporcione un refreshToken válido en el cuerpo de la petición."));
    }
}
