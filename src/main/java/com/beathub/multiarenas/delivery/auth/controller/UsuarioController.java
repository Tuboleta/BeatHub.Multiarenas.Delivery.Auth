package com.beathub.multiarenas.delivery.auth.controller;

import com.beathub.multiarenas.delivery.auth.dto.request.AsignarArenaUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.ApiResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.PagedResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import com.beathub.multiarenas.delivery.auth.service.business.AuthService;
import com.beathub.multiarenas.delivery.auth.service.business.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE')")
    @Operation(
            summary = "Listar todos los usuarios del sistema con filtros y paginación",
            description = "Permite a administradores (SUPER_ADMIN, BACKOFFICE, ADMIN_ARENA) consultar y filtrar usuarios con paginación y búsqueda libre."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista paginada de usuarios obtenida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token JWT faltante, inválido o expirado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permisos para consultar usuarios de la arena indicada")
    })
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> listar(
            @Parameter(description = "Identificador de la arena física para filtrar usuarios asignados", example = "1")
            @RequestParam(value = "arenaId", required = false) Long arenaId,
            @Parameter(description = "Estado del usuario (1 = Activo, 2 = Inactivo)", example = "1")
            @RequestParam(value = "estadoId", required = false) Integer estadoId,
            @Parameter(description = "Filtro por rol (ej. 'RUNNER', 'ADMIN_ARENA', 'CLIENTE', 'SUPER_ADMIN')", example = "RUNNER")
            @RequestParam(value = "rol", required = false) String rol,
            @Parameter(description = "Término de búsqueda libre (username, email, nombres, apellidos, documento)", example = "carlos")
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedResponse<UserProfileResponse> response = usuarioService.listarUsuarios(arenaId, estadoId, rol, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Usuarios listados exitosamente", response));
    }

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
}