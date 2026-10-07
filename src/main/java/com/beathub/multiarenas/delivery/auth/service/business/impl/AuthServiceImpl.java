package com.beathub.multiarenas.delivery.auth.service.business.impl;

import com.beathub.multiarenas.delivery.auth.config.JwtProperties;
import com.beathub.multiarenas.delivery.auth.dto.request.AsignarArenaUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.ClientType;
import com.beathub.multiarenas.delivery.auth.dto.request.LoginRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.RegistroRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.SsoLoginRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.ValidateTokenRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.AuthResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.TokenValidationResponse;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import com.beathub.multiarenas.delivery.auth.entity.*;
import com.beathub.multiarenas.delivery.auth.exception.BusinessException;
import com.beathub.multiarenas.delivery.auth.exception.ForbiddenException;
import com.beathub.multiarenas.delivery.auth.exception.ResourceNotFoundException;
import com.beathub.multiarenas.delivery.auth.exception.UnauthorizedException;
import com.beathub.multiarenas.delivery.auth.repository.*;
import com.beathub.multiarenas.delivery.auth.security.JwtTokenProvider;
import com.beathub.multiarenas.delivery.auth.dto.sso.AzureTokenClaims;
import com.beathub.multiarenas.delivery.auth.service.business.AuthService;
import com.beathub.multiarenas.delivery.auth.service.client.AdsSsoClient;
import com.beathub.multiarenas.delivery.auth.service.client.AzureSsoValidatorService;
import com.beathub.multiarenas.delivery.auth.service.log.AppLoggerService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ArenaUsuarioRepository arenaUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AdsSsoClient adsSsoClient;
    private final AzureSsoValidatorService azureSsoValidatorService;
    private final AppLoggerService appLoggerService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsernameOrEmail(request.getLogin().trim())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        if (usuario.getPassword() == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }

        if (usuario.getEstadoId() != null && usuario.getEstadoId() != 1) {
            throw new UnauthorizedException("El usuario se encuentra inactivo");
        }

        List<Long> arenaIds = usuario.getArenas().stream()
                .map(ArenaUsuario::getArenaId)
                .distinct()
                .collect(Collectors.toList());

        Long defaultArenaId = !arenaIds.isEmpty() ? arenaIds.get(0) : null;
        List<String> roles = obtenerRolesUsuario(usuario, defaultArenaId);
        List<String> scopes = Collections.singletonList("api:access");

        String token = jwtTokenProvider.generarToken(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPersona().getEmail(),
                usuario.getSsoId(),
                usuario.getPersona().getId(),
                roles,
                scopes,
                defaultArenaId,
                arenaIds
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse registro(RegistroRequest request) {
        if (personaRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("El correo electrónico ya se encuentra registrado");
        }

        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("El nombre de usuario ya se encuentra en uso");
        }

        if (personaRepository.existsByTipoDocumentoIdAndNumeroDocumento(request.getTipoDocumentoId(), request.getNumeroDocumento())) {
            throw new BusinessException("El número de documento ya se encuentra registrado");
        }

        Persona persona = Persona.builder()
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .tipoDocumentoId(request.getTipoDocumentoId())
                .numeroDocumento(request.getNumeroDocumento())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .paisId(request.getPaisId())
                .departamentoId(request.getDepartamentoId())
                .ciudadId(request.getCiudadId())
                .direccion(request.getDireccion())
                .arenaId(request.getArenaId())
                .estadoId(1)
                .build();
        persona = personaRepository.save(persona);

        Usuario usuario = Usuario.builder()
                .persona(persona)
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .ssoProvider("TUBOLETA")
                .arenaId(request.getArenaId())
                .estadoId(1)
                .creacionUsuario(1L)
                .build();
        usuario = usuarioRepository.save(usuario);

        // Asignar rol CLIENTE (ID 4) por defecto
        Rol rolCliente = rolRepository.findByNombre("CLIENTE").orElseGet(() ->
                rolRepository.save(Rol.builder().nombre("CLIENTE").descripcion("Cliente App").estadoId(1).build())
        );

        if (request.getArenaId() != null) {
            ArenaUsuario arenaUsuario = ArenaUsuario.builder()
                    .arenaId(request.getArenaId())
                    .usuario(usuario)
                    .estadoId(1)
                    .build();
            RolArenaUsuario rolArena = RolArenaUsuario.builder()
                    .rol(rolCliente)
                    .arenaUsuario(arenaUsuario)
                    .estadoId(1)
                    .arenaId(request.getArenaId())
                    .build();
            arenaUsuario.getRoles().add(rolArena);
            arenaUsuarioRepository.save(arenaUsuario);
            usuario.getArenas().add(arenaUsuario);
        }

        List<String> roles = Collections.singletonList("ROLE_CLIENTE");
        List<String> scopes = Collections.singletonList("api:access");
        List<Long> arenaIds = request.getArenaId() != null ? Collections.singletonList(request.getArenaId()) : Collections.emptyList();

        String token = jwtTokenProvider.generarToken(
                usuario.getId(),
                usuario.getUsername(),
                persona.getEmail(),
                usuario.getSsoId(),
                persona.getId(),
                roles,
                scopes,
                request.getArenaId(),
                arenaIds
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse ssoLogin(SsoLoginRequest request) {
        log.info("==> [SSO LOGIN RECIBIDO] ssoId='{}', email='{}', clientType='{}', arenaId='{}', ssoTokenPresente={}",
                request.getSsoId(), request.getEmail(), request.getClientType(), request.getArenaId(),
                StringUtils.hasText(request.getSsoToken()));

        if (!StringUtils.hasText(request.getSsoToken())) {
            throw new UnauthorizedException("El token de SSO de Azure (ssoToken) es obligatorio");
        }

        OffsetDateTime inicioProceso = OffsetDateTime.now();
        AzureTokenClaims azureClaims;
        try {
            azureClaims = azureSsoValidatorService.validarToken(
                    request.getSsoToken(),
                    request.getSsoId(),
                    request.getEmail()
            );

            appLoggerService.logIntegrationService(
                    request.getArenaId() != null ? String.valueOf(request.getArenaId()) : null,
                    2,
                    "AZURE_SSO_VALIDATE_TOKEN",
                    "ssoId=" + request.getSsoId() + ", email=" + request.getEmail() + ", clientType=" + request.getClientType(),
                    "Validación exitosa para sub=" + azureClaims.getSubject(),
                    inicioProceso,
                    OffsetDateTime.now(),
                    Duration.between(inicioProceso, OffsetDateTime.now()).toMillis(),
                    200,
                    "SUCCESS",
                    1
            );
        } catch (Exception ex) {
            appLoggerService.logIntegrationService(
                    request.getArenaId() != null ? String.valueOf(request.getArenaId()) : null,
                    2,
                    "AZURE_SSO_VALIDATE_TOKEN",
                    "ssoId=" + request.getSsoId() + ", email=" + request.getEmail(),
                    "Fallo de validación: " + ex.getMessage(),
                    inicioProceso,
                    OffsetDateTime.now(),
                    Duration.between(inicioProceso, OffsetDateTime.now()).toMillis(),
                    401,
                    "ERROR",
                    1
            );
            throw ex;
        }

        String ssoId = azureClaims.getSubject().trim();
        String email = StringUtils.hasText(azureClaims.getEmail())
                ? azureClaims.getEmail().trim().toLowerCase()
                : request.getEmail().trim().toLowerCase();
        String ssoProvider = "TUBOLETA";
        ClientType clientType = request.getClientType() != null ? request.getClientType() : ClientType.USER_APP;
        Long targetArenaId = request.getArenaId();

        Optional<Usuario> usuarioOpt = usuarioRepository.findBySsoId(ssoId);
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByPersonaEmail(email);
        }

        Usuario usuario;

        // VALIDACIÓN SEGÚN CLIENT_TYPE
        if (clientType == ClientType.RUNNER_APP) {
            if (usuarioOpt.isEmpty()) {
                throw new ForbiddenException("Acceso denegado: El usuario no se encuentra registrado como Runner en el sistema.");
            }
            usuario = usuarioOpt.get();
            validarEstadoUsuario(usuario);
            vincularSsoSiFalta(usuario, ssoId, ssoProvider);

            boolean esRunner = tieneRolEnArena(usuario, "RUNNER", targetArenaId);
            if (!esRunner) {
                throw new ForbiddenException("Acceso denegado: No cuentas con permisos de Runner asignados para esta arena.");
            }

        } else if (clientType == ClientType.ADMIN_WEB) {
            if (usuarioOpt.isEmpty()) {
                throw new ForbiddenException("Acceso denegado: No tienes permisos administrativos para ingresar al panel.");
            }
            usuario = usuarioOpt.get();
            validarEstadoUsuario(usuario);
            vincularSsoSiFalta(usuario, ssoId, ssoProvider);

            boolean esAdmin = tieneRolEnArena(usuario, "SUPER_ADMIN", null) ||
                    tieneRolEnArena(usuario, "ADMIN_ARENA", targetArenaId);
            if (!esAdmin) {
                throw new ForbiddenException("Acceso denegado: El usuario no cuenta con roles administrativos autorizados.");
            }

        } else { // USER_APP (Clientes)
            if (usuarioOpt.isPresent()) {
                usuario = usuarioOpt.get();
                validarEstadoUsuario(usuario);
                vincularSsoSiFalta(usuario, ssoId, ssoProvider);

                if (targetArenaId != null && !estaVinculadoAArena(usuario, targetArenaId)) {
                    Rol rolCliente = obtenerRolPorNombre("CLIENTE");
                    vincularRolArena(usuario, targetArenaId, rolCliente);
                }
            } else {
                // Auto-aprovisionamiento JIT para cliente nuevo
                Persona persona = personaRepository.findByEmail(email).orElseGet(() -> {
                    String nombres = StringUtils.hasText(azureClaims.getGivenName()) ? azureClaims.getGivenName().trim()
                            : (StringUtils.hasText(azureClaims.getName()) ? azureClaims.getName().trim() : "Usuario");
                    String apellidos = StringUtils.hasText(azureClaims.getFamilyName()) ? azureClaims.getFamilyName().trim() : "TuBoleta";

                    Persona p = Persona.builder()
                            .nombres(nombres)
                            .apellidos(apellidos)
                            .tipoDocumentoId(1)
                            .numeroDocumento(ssoId)
                            .email(email)
                            .arenaId(targetArenaId)
                            .estadoId(1)
                            .build();
                    return personaRepository.save(p);
                });

                String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
                String finalUsername = baseUsername;
                int counter = 1;
                while (usuarioRepository.existsByUsername(finalUsername)) {
                    finalUsername = baseUsername + counter++;
                }

                usuario = Usuario.builder()
                        .persona(persona)
                        .username(finalUsername)
                        .password(null)
                        .ssoId(ssoId)
                        .ssoProvider(ssoProvider)
                        .arenaId(targetArenaId)
                        .estadoId(1)
                        .creacionUsuario(1L)
                        .build();
                usuario = usuarioRepository.save(usuario);

                if (targetArenaId != null) {
                    Rol rolCliente = obtenerRolPorNombre("CLIENTE");
                    vincularRolArena(usuario, targetArenaId, rolCliente);
                }
            }
        }

        List<Long> arenaIds = usuario.getArenas().stream()
                .map(ArenaUsuario::getArenaId)
                .distinct()
                .collect(Collectors.toList());

        Long currentArena = (targetArenaId != null) ? targetArenaId : (!arenaIds.isEmpty() ? arenaIds.get(0) : null);
        List<String> roles = obtenerRolesUsuario(usuario, currentArena);
        List<String> scopes = Collections.singletonList("api:access");

        String token = jwtTokenProvider.generarToken(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPersona().getEmail(),
                usuario.getSsoId(),
                usuario.getPersona().getId(),
                roles,
                scopes,
                currentArena,
                arenaIds
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse asignarArenaUsuario(AsignarArenaUsuarioRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Long arenaId = request.getArenaId();
        Long rolId = request.getRolId();

        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + rolId));

        Optional<Usuario> usuarioOpt = usuarioRepository.findByPersonaEmail(email);
        Usuario usuario;

        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();
        } else {
            // Si el usuario no existe en BeatHub, lo pre-creamos para cuando haga login con su SSO
            Persona persona = personaRepository.findByEmail(email).orElseGet(() -> {
                String nombres = StringUtils.hasText(request.getNombres()) ? request.getNombres().trim() : "Usuario";
                String apellidos = StringUtils.hasText(request.getApellidos()) ? request.getApellidos().trim() : "BeatHub";
                Integer tipoDoc = request.getTipoDocumentoId() != null ? request.getTipoDocumentoId() : 1;
                String numDoc = StringUtils.hasText(request.getNumeroDocumento()) ? request.getNumeroDocumento().trim() : UUID.randomUUID().toString().substring(0, 10);

                Persona p = Persona.builder()
                        .nombres(nombres)
                        .apellidos(apellidos)
                        .tipoDocumentoId(tipoDoc)
                        .numeroDocumento(numDoc)
                        .email(email)
                        .telefono(request.getTelefono())
                        .arenaId(arenaId)
                        .estadoId(1)
                        .build();
                return personaRepository.save(p);
            });

            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "");
            String finalUsername = baseUsername;
            int counter = 1;
            while (usuarioRepository.existsByUsername(finalUsername)) {
                finalUsername = baseUsername + counter++;
            }

            usuario = Usuario.builder()
                    .persona(persona)
                    .username(finalUsername)
                    .password(null)
                    .ssoProvider("TUBOLETA")
                    .arenaId(arenaId)
                    .estadoId(1)
                    .creacionUsuario(1L)
                    .build();
            usuario = usuarioRepository.save(usuario);
        }

        vincularRolArena(usuario, arenaId, rol);

        return obtenerPerfil(usuario.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public TokenValidationResponse validarToken(ValidateTokenRequest request) {
        if (!jwtTokenProvider.validarToken(request.getToken())) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        Claims claims = jwtTokenProvider.extraerClaims(request.getToken());
        Long usuarioId = Long.parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        String email = claims.get("email", String.class);
        String ssoId = claims.get("ssoId", String.class);
        List<String> roles = (List<String>) claims.get("roles", List.class);
        List<String> scopes = (List<String>) claims.get("scopes", List.class);

        Long currentArenaId = null;
        Object rawCurrentArena = claims.get("currentArenaId");
        if (rawCurrentArena instanceof Number num) {
            currentArenaId = num.longValue();
        } else if (rawCurrentArena instanceof String str && !str.isBlank()) {
            try {
                currentArenaId = Long.parseLong(str);
            } catch (NumberFormatException ignored) {
            }
        }

        List<?> rawArenaIds = claims.get("arenaIds", List.class);
        List<Long> arenaIds = new ArrayList<>();
        if (rawArenaIds != null) {
            for (Object item : rawArenaIds) {
                if (item instanceof Number num) {
                    arenaIds.add(num.longValue());
                } else if (item instanceof String str && !str.isBlank()) {
                    try {
                        arenaIds.add(Long.parseLong(str));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return TokenValidationResponse.builder()
                .valid(true)
                .usuarioId(usuarioId)
                .username(username)
                .email(email)
                .ssoId(ssoId)
                .currentArenaId(currentArenaId)
                .roles(roles)
                .scopes(scopes)
                .arenaIds(arenaIds)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse obtenerPerfil(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        Persona persona = usuario.getPersona();
        List<String> roles = obtenerRolesUsuario(usuario, null);
        List<Long> arenaIds = usuario.getArenas().stream()
                .map(ArenaUsuario::getArenaId)
                .distinct()
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .usuarioId(usuario.getId())
                .username(usuario.getUsername())
                .ssoId(usuario.getSsoId())
                .ssoProvider(usuario.getSsoProvider())
                .tcposClientId(usuario.getTcposClientId())
                .estadoId(usuario.getEstadoId())
                .personaId(persona.getId())
                .nombres(persona.getNombres())
                .apellidos(persona.getApellidos())
                .tipoDocumentoId(persona.getTipoDocumentoId())
                .numeroDocumento(persona.getNumeroDocumento())
                .email(persona.getEmail())
                .telefono(persona.getTelefono())
                .paisId(persona.getPaisId())
                .departamentoId(persona.getDepartamentoId())
                .ciudadId(persona.getCiudadId())
                .direccion(persona.getDireccion())
                .roles(roles)
                .scopes(Collections.singletonList("api:access"))
                .arenaIds(arenaIds)
                .creacionFecha(usuario.getCreacionFecha())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(Long usuarioId, Long newArenaId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        if (usuario.getEstadoId() != null && usuario.getEstadoId() != 1) {
            throw new UnauthorizedException("El usuario se encuentra inactivo");
        }

        List<Long> arenaIds = usuario.getArenas().stream()
                .map(ArenaUsuario::getArenaId)
                .distinct()
                .collect(Collectors.toList());

        Long effectiveArena = newArenaId;
        if (effectiveArena != null && !arenaIds.contains(effectiveArena) && !tieneRolEnArena(usuario, "SUPER_ADMIN", null)) {
            throw new ForbiddenException("No tienes permisos para operar en la arena solicitada.");
        }
        if (effectiveArena == null && !arenaIds.isEmpty()) {
            effectiveArena = arenaIds.get(0);
        }

        List<String> roles = obtenerRolesUsuario(usuario, effectiveArena);
        List<String> scopes = Collections.singletonList("api:access");

        String token = jwtTokenProvider.generarToken(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPersona().getEmail(),
                usuario.getSsoId(),
                usuario.getPersona().getId(),
                roles,
                scopes,
                effectiveArena,
                arenaIds
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .build();
    }

    private void validarEstadoUsuario(Usuario usuario) {
        if (usuario.getEstadoId() != null && usuario.getEstadoId() != 1) {
            throw new UnauthorizedException("El usuario se encuentra inactivo");
        }
    }

    private void vincularSsoSiFalta(Usuario usuario, String ssoId, String ssoProvider) {
        if (usuario.getSsoId() == null || !usuario.getSsoId().equals(ssoId)) {
            usuario.setSsoId(ssoId);
            usuario.setSsoProvider(ssoProvider);
            usuarioRepository.save(usuario);
        }
    }

    private boolean estaVinculadoAArena(Usuario usuario, Long arenaId) {
        if (usuario.getArenas() == null) return false;
        return usuario.getArenas().stream().anyMatch(au -> arenaId.equals(au.getArenaId()));
    }

    private boolean tieneRolEnArena(Usuario usuario, String rolBuscado, Long arenaId) {
        if (usuario.getArenas() == null) return false;
        String rolFormateado = "ROLE_" + rolBuscado.toUpperCase().replace("ROLE_", "");

        for (ArenaUsuario au : usuario.getArenas()) {
            if (au.getRoles() != null) {
                for (RolArenaUsuario rau : au.getRoles()) {
                    if (rau.getRol() != null && rau.getRol().getNombre() != null) {
                        String nombre = "ROLE_" + rau.getRol().getNombre().toUpperCase().replace("ROLE_", "");
                        if (nombre.equals("ROLE_SUPER_ADMIN")) {
                            return true;
                        }
                        if (nombre.equals(rolFormateado)) {
                            if (arenaId == null || arenaId.equals(au.getArenaId())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private Rol obtenerRolPorNombre(String rolNombre) {
        return rolRepository.findByNombre(rolNombre).orElseGet(() ->
                rolRepository.save(Rol.builder().nombre(rolNombre).descripcion("Rol " + rolNombre).estadoId(1).build())
        );
    }

    private void vincularRolArena(Usuario usuario, Long arenaId, Rol rol) {
        Optional<ArenaUsuario> arenaUsuarioOpt = usuario.getArenas().stream()
                .filter(au -> arenaId.equals(au.getArenaId()))
                .findFirst();

        ArenaUsuario arenaUsuario;
        if (arenaUsuarioOpt.isPresent()) {
            arenaUsuario = arenaUsuarioOpt.get();
        } else {
            arenaUsuario = ArenaUsuario.builder()
                    .arenaId(arenaId)
                    .usuario(usuario)
                    .estadoId(1)
                    .build();
            arenaUsuario = arenaUsuarioRepository.save(arenaUsuario);
            usuario.getArenas().add(arenaUsuario);
        }

        boolean yaTieneRol = arenaUsuario.getRoles().stream()
                .anyMatch(r -> r.getRol() != null && rol.getId() != null && rol.getId().equals(r.getRol().getId()));

        if (!yaTieneRol) {
            RolArenaUsuario rolArena = RolArenaUsuario.builder()
                    .rol(rol)
                    .arenaUsuario(arenaUsuario)
                    .estadoId(1)
                    .arenaId(arenaId)
                    .build();
            arenaUsuario.getRoles().add(rolArena);
            arenaUsuarioRepository.save(arenaUsuario);
        }
    }

    private List<String> obtenerRolesUsuario(Usuario usuario, Long arenaId) {
        List<String> roles = new ArrayList<>();
        if (usuario.getArenas() != null) {
            for (ArenaUsuario au : usuario.getArenas()) {
                if (arenaId == null || arenaId.equals(au.getArenaId())) {
                    if (au.getRoles() != null) {
                        for (RolArenaUsuario rau : au.getRoles()) {
                            if (rau.getRol() != null && rau.getRol().getNombre() != null) {
                                String r = rau.getRol().getNombre().toUpperCase();
                                if (!r.startsWith("ROLE_")) {
                                    r = "ROLE_" + r;
                                }
                                roles.add(r);
                            }
                        }
                    }
                }
            }
        }
        if (roles.isEmpty()) {
            roles.add("ROLE_CLIENTE");
        }
        return roles.stream().distinct().collect(Collectors.toList());
    }
}
