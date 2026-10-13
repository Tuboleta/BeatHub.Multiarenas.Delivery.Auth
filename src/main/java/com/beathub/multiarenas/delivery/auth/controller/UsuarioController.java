package com.beathub.multiarenas.delivery.auth.controller;

import com.beathub.multiarenas.delivery.auth.dto.request.AsignarArenaUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.ApiResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import com.beathub.multiarenas.delivery.auth.service.business.AuthService;
import com.beathub.multiarenas.delivery.auth.service.business.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Administración y consulta de usuarios, perfiles y asignación multi-arena")
@SecurityRequirement(name = "Bearer Authentication")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    @PostMapping("/asignar-arena")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE')")
    @Operation(summary = "Asignar rol y arena a un usuario (Dar de alta Runner o Admin de Arena)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> asignarArena(@Valid @RequestBody AsignarArenaUsuarioRequest request) {
        UserProfileResponse response = authService.asignarArenaUsuario(request);
        return ResponseEntity.ok(ApiResponse.success("Usuario asignado a la arena exitosamente", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE') or #id == authentication.principal.usuarioId")
    @Operation(summary = "Obtener usuario por ID")
    public ResponseEntity<ApiResponse<UserProfileResponse>> obtenerPorId(@PathVariable("id") Long id) {
        UserProfileResponse response = usuarioService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.success("Usuario encontrado", response));
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE') or #username == authentication.principal.username")
    @Operation(summary = "Obtener usuario por username")
    public ResponseEntity<ApiResponse<UserProfileResponse>> obtenerPorUsername(@PathVariable("username") String username) {
        UserProfileResponse response = usuarioService.obtenerPorUsername(username);
        return ResponseEntity.ok(ApiResponse.success("Usuario encontrado", response));
    }
}
