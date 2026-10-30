package com.beathub.multiarenas.delivery.auth.controller;

import com.beathub.multiarenas.delivery.auth.dto.request.ActualizarShopUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.ActualizarUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.AsignarArenaUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.CrearUsuarioRequest;
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
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE')")
    @Operation(summary = "Crear nuevo usuario desde BackOffice",
               description = "Permite registrar un usuario con perfil de persona, credenciales opcionales y asignación inicial de rol, arena y barra física.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o faltantes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token JWT faltante, inválido o expirado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permisos para registrar usuarios en la arena especificada o con el rol solicitado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol o Arena no encontrados"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Regla de negocio no satisfecha (ej. correo o documento ya registrados)")
    })
    public ResponseEntity<ApiResponse<UserProfileResponse>> crearUsuario(
            @Valid @RequestBody CrearUsuarioRequest request) {
        UserProfileResponse response = usuarioService.crearUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuario creado exitosamente", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE')")
    @Operation(summary = "Listar todos los usuarios del sistema con filtros y paginación", description = "Permite a administradores (SUPER_ADMIN, BACKOFFICE, ADMIN_ARENA) consultar y filtrar usuarios con paginación y búsqueda libre.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista paginada de usuarios obtenida exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token JWT faltante, inválido o expirado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "No tiene permisos para consultar usuarios de la arena indicada")
    })
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> listar(
            @Parameter(description = "Identificador de la arena física para filtrar usuarios asignados", example = "1") @RequestParam(value = "arenaId", required = false) Long arenaId,
            @Parameter(description = "Estado del usuario (1 = Activo, 2 = Inactivo)", example = "1") @RequestParam(value = "estadoId", required = false) Integer estadoId,
            @Parameter(description = "Filtro por rol (ej. 'RUNNER', 'ADMIN_ARENA', 'CLIENTE', 'SUPER_ADMIN')", example = "RUNNER") @RequestParam(value = "rol", required = false) String rol,
            @Parameter(description = "Término de búsqueda libre (username, email, nombres, apellidos, documento)", example = "carlos") @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedResponse<UserProfileResponse> response = usuarioService.listarUsuarios(arenaId, estadoId, rol, search,
                pageable);
        return ResponseEntity.ok(ApiResponse.success("Usuarios listados exitosamente", response));
    }

    @PostMapping("/asignar-arena")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE')")
    @Operation(summary = "Asignar rol y arena a un usuario (Dar de alta Runner o Admin de Arena)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> asignarArena(
            @Valid @RequestBody AsignarArenaUsuarioRequest request) {
        UserProfileResponse response = authService.asignarArenaUsuario(request);
        return ResponseEntity.ok(ApiResponse.success("Usuario asignado a la arena exitosamente", response));
    }

    @PutMapping("/{usuarioId}/arenas/{arenaId}/shop")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE')")
    @Operation(summary = "Asignar o reasignar punto de venta / shop a un usuario en una arena",
               description = "Permite a supervisores o administradores vincular, rotar o desvincular (shopId null) la barra asignada a un operador.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> actualizarShopUsuario(
            @PathVariable("usuarioId") Long usuarioId,
            @PathVariable("arenaId") Long arenaId,
            @Valid @RequestBody ActualizarShopUsuarioRequest request) {
        UserProfileResponse response = authService.actualizarShopUsuario(usuarioId, arenaId, request);
        return ResponseEntity.ok(ApiResponse.success("Punto de venta actualizado exitosamente para el usuario en la arena", response));
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
    public ResponseEntity<ApiResponse<UserProfileResponse>> obtenerPorUsername(
            @PathVariable("username") String username) {
        UserProfileResponse response = usuarioService.obtenerPorUsername(username);
        return ResponseEntity.ok(ApiResponse.success("Usuario encontrado", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE')")
    @Operation(summary = "Editar usuario desde BackOffice",
               description = "Permite actualizar datos de persona, credenciales, estado y asignaciones de arena, rol y barra.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token JWT inválido o expirado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sin permisos para editar el usuario"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario, Rol o Arena no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Conflicto o incompatibilidad de roles (RUNNER vs BARRA)")
    })
    public ResponseEntity<ApiResponse<UserProfileResponse>> actualizarUsuario(
            @PathVariable("id") Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        UserProfileResponse response = usuarioService.actualizarUsuario(id, request);
        return ResponseEntity.ok(ApiResponse.success("Usuario actualizado exitosamente", response));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE')")
    @Operation(summary = "Cambiar estado de un usuario (Activar/Inactivar)",
               description = "Permite modificar el estado operativo (1 = Activo, 2 = Inactivo).")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(
            @PathVariable("id") Long id,
            @Parameter(description = "Nuevo estado (1 = Activo, 2 = Inactivo)", example = "2") @RequestParam("estadoId") Integer estadoId) {
        usuarioService.cambiarEstado(id, estadoId);
        return ResponseEntity.ok(ApiResponse.success("Estado de usuario actualizado exitosamente", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_ARENA', 'BACKOFFICE')")
    @Operation(summary = "Inactivar usuario (Soft delete)",
               description = "Inactiva el usuario estableciendo su estadoId en 2.")
    public ResponseEntity<ApiResponse<Void>> inactivarUsuario(@PathVariable("id") Long id) {
        usuarioService.cambiarEstado(id, 2);
        return ResponseEntity.ok(ApiResponse.success("Usuario inactivado exitosamente", null));
    }
}
