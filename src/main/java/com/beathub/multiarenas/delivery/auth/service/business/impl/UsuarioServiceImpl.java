package com.beathub.multiarenas.delivery.auth.service.business.impl;

import com.beathub.multiarenas.delivery.auth.dto.request.ActualizarUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.AsignarRunnerZonasRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.CrearUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.PagedResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.RunnerDisponibleResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.RunnerZonaDto;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import com.beathub.multiarenas.delivery.auth.entity.ArenaUsuario;
import com.beathub.multiarenas.delivery.auth.entity.Persona;
import com.beathub.multiarenas.delivery.auth.entity.Rol;
import com.beathub.multiarenas.delivery.auth.entity.RolArenaUsuario;
import com.beathub.multiarenas.delivery.auth.entity.RunnerZona;
import com.beathub.multiarenas.delivery.auth.entity.Usuario;
import com.beathub.multiarenas.delivery.auth.exception.BadRequestException;
import com.beathub.multiarenas.delivery.auth.exception.BusinessException;
import com.beathub.multiarenas.delivery.auth.exception.ForbiddenException;
import com.beathub.multiarenas.delivery.auth.exception.ResourceNotFoundException;
import com.beathub.multiarenas.delivery.auth.repository.ArenaUsuarioRepository;
import com.beathub.multiarenas.delivery.auth.repository.PersonaRepository;
import com.beathub.multiarenas.delivery.auth.repository.RolRepository;
import com.beathub.multiarenas.delivery.auth.repository.RunnerZonaRepository;
import com.beathub.multiarenas.delivery.auth.repository.UsuarioRepository;
import com.beathub.multiarenas.delivery.auth.repository.specification.UsuarioSpecification;
import com.beathub.multiarenas.delivery.auth.security.SecurityUtils;
import com.beathub.multiarenas.delivery.auth.service.business.AuthService;
import com.beathub.multiarenas.delivery.auth.service.business.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.beathub.multiarenas.delivery.auth.config.RedisCacheConfig.CACHE_USER_PROFILE;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final ArenaUsuarioRepository arenaUsuarioRepository;
    private final RunnerZonaRepository runnerZonaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Override
    @Transactional
    @CacheEvict(value = CACHE_USER_PROFILE, allEntries = true)
    public UserProfileResponse crearUsuario(CrearUsuarioRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Validaciones de unicidad de persona
        if (personaRepository.existsByEmail(email)) {
            throw new BusinessException("El correo electrónico ya se encuentra registrado: " + email);
        }

        if (personaRepository.existsByTipoDocumentoIdAndNumeroDocumento(request.getTipoDocumentoId(),
                request.getNumeroDocumento().trim())) {
            throw new BusinessException(
                    "El número de documento ya se encuentra registrado para el tipo de documento especificado.");
        }

        // 2. Determinar username y validar unicidad
        String username;
        if (StringUtils.hasText(request.getUsername())) {
            username = request.getUsername().trim();
            if (usuarioRepository.existsByUsername(username)) {
                throw new BusinessException("El nombre de usuario '" + username + "' ya se encuentra en uso.");
            }
        } else {
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
            username = baseUsername;
            int counter = 1;
            while (usuarioRepository.existsByUsername(username)) {
                username = baseUsername + counter++;
            }
        }

        // 3. Resolución de rol
        Rol rol = null;
        if (request.getRolId() != null) {
            rol = rolRepository.findById(request.getRolId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Rol no encontrado con ID: " + request.getRolId()));
        } else if (StringUtils.hasText(request.getRolNombre())) {
            String nombreRolLimpio = request.getRolNombre().trim().toUpperCase().replace("ROLE_", "");
            rol = rolRepository.findByNombre(nombreRolLimpio)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rol no encontrado con nombre: " + request.getRolNombre()));
        }

        // 4. Multi-Tenant y Seguridad según rol del operador autenticado
        boolean esSuperAdminOBackoffice = SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.hasRole("BACKOFFICE");
        Long arenaId = request.getArenaId();

        if (!esSuperAdminOBackoffice) {
            // ADMIN_ARENA
            if (rol != null) {
                String rolUpper = rol.getNombre().toUpperCase();
                if (rolUpper.equals("SUPER_ADMIN") || rolUpper.equals("BACKOFFICE")) {
                    throw new ForbiddenException(
                            "No tiene permisos para crear usuarios con roles administrativos globales.");
                }
            }

            if (arenaId != null) {
                if (!SecurityUtils.hasArenaAccess(arenaId)) {
                    throw new ForbiddenException(
                            "No tiene permisos para registrar usuarios en la arena con ID: " + arenaId);
                }
            } else {
                arenaId = SecurityUtils.getCurrentArenaId()
                        .orElseThrow(() -> new ForbiddenException(
                                "Debe especificar una arena física para registrar el usuario."));
            }
        }

        if (arenaId == null && rol != null) {
            String rolUpper = rol.getNombre().toUpperCase();
            if (rolUpper.equals("SUPER_ADMIN") || rolUpper.equals("BACKOFFICE")) {
                arenaId = 1L; // Asignación de arena raíz para roles globales
            } else {
                throw new BadRequestException(
                        "Debe especificar 'arenaId' para vincular el rol " + rol.getNombre() + " a una arena física.");
            }
        }

        // 5. Preparación de credenciales y auditoría
        Long auditUserId = SecurityUtils.getCurrentUsuarioId().orElse(1L);
        Integer estadoId = request.getEstadoId() != null ? request.getEstadoId() : 1;
        String encodedPassword = StringUtils.hasText(request.getPassword())
                ? passwordEncoder.encode(request.getPassword())
                : null;
        String ssoProvider = StringUtils.hasText(request.getSsoProvider())
                ? request.getSsoProvider().trim().toUpperCase()
                : (encodedPassword != null ? "LOCAL" : "TUBOLETA");

        // 6. Guardar Persona
        Persona persona = Persona.builder()
                .nombres(request.getNombres().trim())
                .apellidos(request.getApellidos().trim())
                .tipoDocumentoId(request.getTipoDocumentoId())
                .numeroDocumento(request.getNumeroDocumento().trim())
                .email(email)
                .telefono(StringUtils.hasText(request.getTelefono()) ? request.getTelefono().trim() : null)
                .paisId(request.getPaisId())
                .departamentoId(request.getDepartamentoId())
                .ciudadId(request.getCiudadId())
                .direccion(StringUtils.hasText(request.getDireccion()) ? request.getDireccion().trim() : null)
                .arenaId(arenaId)
                .estadoId(estadoId)
                .creacionUsuario(auditUserId)
                .build();
        persona = personaRepository.save(persona);

        // 7. Guardar Usuario
        Usuario usuario = Usuario.builder()
                .persona(persona)
                .username(username)
                .password(encodedPassword)
                .ssoId(StringUtils.hasText(request.getSsoId()) ? request.getSsoId().trim() : null)
                .ssoProvider(ssoProvider)
                .tcposClientId(
                        StringUtils.hasText(request.getTcposClientId()) ? request.getTcposClientId().trim() : null)
                .arenaId(arenaId)
                .estadoId(estadoId)
                .creacionUsuario(auditUserId)
                .build();
        usuario = usuarioRepository.save(usuario);

        // 8. Vincular Arena, Rol y Barra (Shop) si aplica
        if (arenaId != null) {
            ArenaUsuario arenaUsuario = ArenaUsuario.builder()
                    .arenaId(arenaId)
                    .usuario(usuario)
                    .shopId(request.getShopId())
                    .shopNombre(StringUtils.hasText(request.getShopNombre()) ? request.getShopNombre().trim() : null)
                    .estadoId(estadoId)
                    .creacionUsuario(auditUserId)
                    .build();

            if (rol != null) {
                gestionarAsignacionRolOperativo(arenaUsuario, rol, false, auditUserId);
            }

            arenaUsuario = arenaUsuarioRepository.save(arenaUsuario);
            usuario.getArenas().add(arenaUsuario);
        }

        return authService.mapearUsuarioAProfileResponse(usuario);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_USER_PROFILE, allEntries = true)
    public UserProfileResponse actualizarUsuario(Long usuarioId, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));
        Persona persona = usuario.getPersona();

        boolean esSuperAdminOBackoffice = SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.hasRole("BACKOFFICE");
        if (!esSuperAdminOBackoffice) {
            Long adminArenaId = SecurityUtils.getCurrentArenaId()
                    .orElseThrow(() -> new ForbiddenException("No se pudo resolver la arena del administrador."));

            boolean usuarioEnArena = usuario.getArenas().stream()
                    .anyMatch(au -> adminArenaId.equals(au.getArenaId()));
            if (!usuarioEnArena) {
                throw new ForbiddenException(
                        "No tiene permisos para modificar un usuario que no pertenece a su arena.");
            }
            if (request.getArenaId() != null && !adminArenaId.equals(request.getArenaId())) {
                throw new ForbiddenException("No tiene permisos para transferir usuarios a otra arena física.");
            }
        }

        Long auditUserId = SecurityUtils.getCurrentUsuarioId().orElse(1L);

        // 1. Modificar Persona
        if (StringUtils.hasText(request.getEmail())) {
            String nuevoEmail = request.getEmail().trim().toLowerCase();
            if (!nuevoEmail.equalsIgnoreCase(persona.getEmail())) {
                personaRepository.findByEmail(nuevoEmail).ifPresent(p -> {
                    if (!p.getId().equals(persona.getId())) {
                        throw new BusinessException(
                                "El correo electrónico ya se encuentra registrado por otro usuario: " + nuevoEmail);
                    }
                });
                persona.setEmail(nuevoEmail);
            }
        }

        if (request.getTipoDocumentoId() != null || StringUtils.hasText(request.getNumeroDocumento())) {
            Integer tipoDoc = request.getTipoDocumentoId() != null ? request.getTipoDocumentoId()
                    : persona.getTipoDocumentoId();
            String numDoc = StringUtils.hasText(request.getNumeroDocumento()) ? request.getNumeroDocumento().trim()
                    : persona.getNumeroDocumento();

            personaRepository.findByTipoDocumentoIdAndNumeroDocumento(tipoDoc, numDoc).ifPresent(p -> {
                if (!p.getId().equals(persona.getId())) {
                    throw new BusinessException("El número de documento ya se encuentra registrado por otro usuario.");
                }
            });
            persona.setTipoDocumentoId(tipoDoc);
            persona.setNumeroDocumento(numDoc);
        }

        if (StringUtils.hasText(request.getNombres())) {
            persona.setNombres(request.getNombres().trim());
        }
        if (StringUtils.hasText(request.getApellidos())) {
            persona.setApellidos(request.getApellidos().trim());
        }
        if (request.getTelefono() != null) {
            persona.setTelefono(StringUtils.hasText(request.getTelefono()) ? request.getTelefono().trim() : null);
        }
        if (request.getPaisId() != null) {
            persona.setPaisId(request.getPaisId());
        }
        if (request.getDepartamentoId() != null) {
            persona.setDepartamentoId(request.getDepartamentoId());
        }
        if (request.getCiudadId() != null) {
            persona.setCiudadId(request.getCiudadId());
        }
        if (request.getDireccion() != null) {
            persona.setDireccion(StringUtils.hasText(request.getDireccion()) ? request.getDireccion().trim() : null);
        }
        persona.setActualizacionUsuario(auditUserId);
        personaRepository.save(persona);

        // 2. Modificar Usuario
        final Long currentUsuarioId = usuario.getId();
        if (StringUtils.hasText(request.getUsername())) {
            String nuevoUsername = request.getUsername().trim();
            if (!nuevoUsername.equalsIgnoreCase(usuario.getUsername())) {
                usuarioRepository.findByUsername(nuevoUsername).ifPresent(u -> {
                    if (!u.getId().equals(currentUsuarioId)) {
                        throw new BusinessException(
                                "El nombre de usuario '" + nuevoUsername + "' ya se encuentra en uso.");
                    }
                });
                usuario.setUsername(nuevoUsername);
            }
        }
        if (StringUtils.hasText(request.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
            if ("TUBOLETA".equalsIgnoreCase(usuario.getSsoProvider())) {
                usuario.setSsoProvider("LOCAL");
            }
        }
        if (request.getEstadoId() != null) {
            usuario.setEstadoId(request.getEstadoId());
            persona.setEstadoId(request.getEstadoId());
            personaRepository.save(persona);
        }
        if (request.getTcposClientId() != null) {
            usuario.setTcposClientId(
                    StringUtils.hasText(request.getTcposClientId()) ? request.getTcposClientId().trim() : null);
        }
        usuario.setActualizacionUsuario(auditUserId);

        // 3. Modificar / Asignar Arena, Rol y Barra (Shop) si aplica
        Long targetArenaId = request.getArenaId();
        if (targetArenaId != null || request.getRolId() != null || StringUtils.hasText(request.getRolNombre())
                || request.getShopId() != null) {
            final Long arenaId = targetArenaId != null ? targetArenaId
                    : (usuario.getArenaId() != null ? usuario.getArenaId() : 1L);

            Rol nuevoRol = null;
            if (request.getRolId() != null) {
                nuevoRol = rolRepository.findById(request.getRolId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Rol no encontrado con ID: " + request.getRolId()));
            } else if (StringUtils.hasText(request.getRolNombre())) {
                String nombreRolLimpio = request.getRolNombre().trim().toUpperCase().replace("ROLE_", "");
                nuevoRol = rolRepository.findByNombre(nombreRolLimpio)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Rol no encontrado con nombre: " + request.getRolNombre()));
            }

            if (!esSuperAdminOBackoffice && nuevoRol != null) {
                String rolUpper = nuevoRol.getNombre().toUpperCase();
                if (rolUpper.equals("SUPER_ADMIN") || rolUpper.equals("BACKOFFICE")) {
                    throw new ForbiddenException("No tiene permisos para asignar roles administrativos globales.");
                }
            }

            final Usuario usuarioRef = usuario;
            ArenaUsuario arenaUsuario = usuario.getArenas().stream()
                    .filter(au -> arenaId.equals(au.getArenaId()))
                    .findFirst()
                    .orElseGet(() -> {
                        ArenaUsuario nuevoAu = ArenaUsuario.builder()
                                .arenaId(arenaId)
                                .usuario(usuarioRef)
                                .estadoId(1)
                                .creacionUsuario(auditUserId)
                                .build();
                        usuarioRef.getArenas().add(nuevoAu);
                        return nuevoAu;
                    });

            if (request.getShopId() != null) {
                arenaUsuario.setShopId(request.getShopId());
            }
            if (StringUtils.hasText(request.getShopNombre())) {
                arenaUsuario.setShopNombre(request.getShopNombre().trim());
            }

            if (nuevoRol != null) {
                boolean reemplazar = request.getReemplazarRolOperativo() == null
                        || Boolean.TRUE.equals(request.getReemplazarRolOperativo());
                gestionarAsignacionRolOperativo(arenaUsuario, nuevoRol, reemplazar, auditUserId);
            }

            arenaUsuario.setActualizacionUsuario(auditUserId);
            arenaUsuarioRepository.save(arenaUsuario);
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        return authService.mapearUsuarioAProfileResponse(usuarioActualizado);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_USER_PROFILE, allEntries = true)
    public void cambiarEstado(Long usuarioId, Integer nuevoEstadoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        boolean esSuperAdminOBackoffice = SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.hasRole("BACKOFFICE");
        if (!esSuperAdminOBackoffice) {
            Long adminArenaId = SecurityUtils.getCurrentArenaId()
                    .orElseThrow(() -> new ForbiddenException("No se pudo resolver la arena del administrador."));
            boolean usuarioEnArena = usuario.getArenas().stream()
                    .anyMatch(au -> adminArenaId.equals(au.getArenaId()));
            if (!usuarioEnArena) {
                throw new ForbiddenException(
                        "No tiene permisos para modificar el estado de un usuario que no pertenece a su arena.");
            }
        }

        Long auditUserId = SecurityUtils.getCurrentUsuarioId().orElse(1L);
        usuario.setEstadoId(nuevoEstadoId);
        usuario.setActualizacionUsuario(auditUserId);

        if (usuario.getPersona() != null) {
            usuario.getPersona().setEstadoId(nuevoEstadoId);
            usuario.getPersona().setActualizacionUsuario(auditUserId);
            personaRepository.save(usuario.getPersona());
        }

        usuarioRepository.save(usuario);
    }

    private void gestionarAsignacionRolOperativo(ArenaUsuario arenaUsuario, Rol nuevoRol, boolean reemplazar,
            Long auditUserId) {
        String nuevoRolLimpio = nuevoRol.getNombre().toUpperCase().replace("ROLE_", "");
        boolean esOperativoPWA = nuevoRolLimpio.equals("RUNNER") || nuevoRolLimpio.equals("BARRA");

        if (esOperativoPWA) {
            String rolOpuesto = nuevoRolLimpio.equals("RUNNER") ? "BARRA" : "RUNNER";

            Optional<RolArenaUsuario> rolOpuestoExistente = arenaUsuario.getRoles().stream()
                    .filter(r -> r.getRol() != null && r.getRol().getNombre() != null
                            && r.getRol().getNombre().toUpperCase().replace("ROLE_", "").equals(rolOpuesto))
                    .findFirst();

            if (rolOpuestoExistente.isPresent()) {
                if (reemplazar) {
                    arenaUsuario.getRoles().remove(rolOpuestoExistente.get());
                    if (nuevoRolLimpio.equals("RUNNER")) {
                        arenaUsuario.setShopId(null);
                        arenaUsuario.setShopNombre(null);
                    }
                } else {
                    throw new BusinessException(String.format(
                            "Incompatibilidad de roles en la PWA: Un usuario no puede tener simultáneamente los roles de RUNNER y BARRA en la misma arena (Arena ID: %d).",
                            arenaUsuario.getArenaId()));
                }
            }
        }

        boolean yaTieneRol = arenaUsuario.getRoles().stream()
                .anyMatch(r -> r.getRol() != null && nuevoRol.getId() != null
                        && nuevoRol.getId().equals(r.getRol().getId()));

        if (!yaTieneRol) {
            RolArenaUsuario nuevoRolArena = RolArenaUsuario.builder()
                    .rol(nuevoRol)
                    .arenaUsuario(arenaUsuario)
                    .estadoId(1)
                    .arenaId(arenaUsuario.getArenaId())
                    .creacionUsuario(auditUserId)
                    .build();
            arenaUsuario.getRoles().add(nuevoRolArena);
        }
    }

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
    public PagedResponse<UserProfileResponse> listarUsuarios(Long arenaId, Integer estadoId, String rol, String search,
            Pageable pageable) {
        Long arenaFiltro = arenaId;
        boolean esSuperAdminOBackoffice = SecurityUtils.hasRole("SUPER_ADMIN") || SecurityUtils.hasRole("BACKOFFICE");

        if (!esSuperAdminOBackoffice) {
            if (arenaFiltro != null) {
                if (!SecurityUtils.hasArenaAccess(arenaFiltro)) {
                    throw new ForbiddenException(
                            "No tiene permisos para consultar usuarios de la arena " + arenaFiltro);
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

    @Override
    @Transactional
    @CacheEvict(value = CACHE_USER_PROFILE, allEntries = true)
    public UserProfileResponse asignarZonasARunner(Long usuarioId, Long arenaId, AsignarRunnerZonasRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        ArenaUsuario arenaUsuario = usuario.getArenas().stream()
                .filter(au -> au.getArenaId().equals(arenaId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("El usuario no está asignado a la arena ID: " + arenaId));

        boolean tieneRolRunner = arenaUsuario.getRoles().stream()
                .anyMatch(r -> r.getRol() != null && "RUNNER".equalsIgnoreCase(r.getRol().getNombre().replace("ROLE_", "")));

        if (!tieneRolRunner) {
            throw new BusinessException("El usuario debe tener el rol RUNNER asignado en la arena para configurar zonas de cobertura.");
        }

        if (request.getZonas() != null && request.getZonas().size() > 3) {
            throw new BusinessException("Un runner no puede tener más de 3 zonas asignadas simultáneamente.");
        }

        // Eliminar asignaciones previas en esta arena
        runnerZonaRepository.deleteByUsuarioIdAndArenaId(usuarioId, arenaId);

        if (request.getZonas() != null && !request.getZonas().isEmpty()) {
            List<RunnerZona> nuevasZonas = new ArrayList<>();
            for (RunnerZonaDto dto : request.getZonas()) {
                if (dto.getZonaId() != null) {
                    RunnerZona rz = RunnerZona.builder()
                            .usuario(usuario)
                            .arenaId(arenaId)
                            .zonaId(dto.getZonaId())
                            .prioridad(dto.getPrioridad() != null ? dto.getPrioridad() : 1)
                            .activo(dto.getActivo() != null ? dto.getActivo() : true)
                            .estadoId(1)
                            .build();
                    nuevasZonas.add(rz);
                }
            }
            runnerZonaRepository.saveAll(nuevasZonas);
        }

        UserProfileResponse response = authService.obtenerPerfil(usuarioId);
        response.setRunnerZonas(obtenerZonasDeRunner(usuarioId, arenaId));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunnerZonaDto> obtenerZonasDeRunner(Long usuarioId, Long arenaId) {
        return runnerZonaRepository.findByUsuarioIdAndArenaIdAndActivoTrueOrderByPrioridadAsc(usuarioId, arenaId)
                .stream()
                .map(rz -> RunnerZonaDto.builder()
                        .zonaId(rz.getZonaId())
                        .prioridad(rz.getPrioridad())
                        .activo(rz.getActivo())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunnerDisponibleResponse> listarRunnersDisponibles(Long arenaId, Long zonaId) {
        List<RunnerZona> runnersZonas = Collections.emptyList();
        if (zonaId != null) {
            runnersZonas = runnerZonaRepository.findActiveRunnersByArenaAndZona(arenaId, zonaId);
        }

        // Si no hay zonificados para esa zona específica, buscar si hay runners zonificados en la arena
        if (runnersZonas.isEmpty()) {
            runnersZonas = runnerZonaRepository.findByArenaIdAndActivoTrue(arenaId);
        }

        Map<Long, List<RunnerZona>> porUsuario = runnersZonas.stream()
                .filter(rz -> rz.getUsuario() != null && Integer.valueOf(1).equals(rz.getUsuario().getEstadoId()))
                .collect(Collectors.groupingBy(rz -> rz.getUsuario().getId()));

        List<RunnerDisponibleResponse> resultado = new ArrayList<>();
        for (Map.Entry<Long, List<RunnerZona>> entry : porUsuario.entrySet()) {
            List<RunnerZona> lista = entry.getValue();
            RunnerZona primero = lista.get(0);
            Usuario u = primero.getUsuario();
            Persona p = u.getPersona();

            Integer prioridadConsultada = (zonaId != null)
                    ? lista.stream()
                            .filter(rz -> rz.getZonaId().equals(zonaId))
                            .map(RunnerZona::getPrioridad)
                            .findFirst()
                            .orElse(3)
                    : 1;

            List<RunnerZonaDto> todasZonas = lista.stream()
                    .map(rz -> RunnerZonaDto.builder()
                            .zonaId(rz.getZonaId())
                            .prioridad(rz.getPrioridad())
                            .activo(rz.getActivo())
                            .build())
                    .collect(Collectors.toList());

            resultado.add(RunnerDisponibleResponse.builder()
                    .usuarioId(u.getId())
                    .username(u.getUsername())
                    .nombres(p != null ? p.getNombres() : u.getUsername())
                    .apellidos(p != null ? p.getApellidos() : "")
                    .telefono(p != null ? p.getTelefono() : "")
                    .prioridadZona(prioridadConsultada)
                    .disponible(true)
                    .zonasAsignadas(todasZonas)
                    .build());
        }

        // Si todavía no hay runners porque ningún runner ha sido zonificado en runner_zona,
        // fallback a cualquier runner activo registrado en la arena
        if (resultado.isEmpty()) {
            List<Usuario> runnersGenerales = usuarioRepository.findAll(
                    com.beathub.multiarenas.delivery.auth.repository.specification.UsuarioSpecification.conFiltros(arenaId, 1, "RUNNER", null)
            );
            for (Usuario u : runnersGenerales) {
                Persona p = u.getPersona();
                resultado.add(RunnerDisponibleResponse.builder()
                        .usuarioId(u.getId())
                        .username(u.getUsername())
                        .nombres(p != null ? p.getNombres() : u.getUsername())
                        .apellidos(p != null ? p.getApellidos() : "")
                        .telefono(p != null ? p.getTelefono() : "")
                        .prioridadZona(1)
                        .disponible(true)
                        .zonasAsignadas(Collections.emptyList())
                        .build());
            }
        }

        resultado.sort(Comparator.comparing(RunnerDisponibleResponse::getPrioridadZona));
        return resultado;
    }
}
