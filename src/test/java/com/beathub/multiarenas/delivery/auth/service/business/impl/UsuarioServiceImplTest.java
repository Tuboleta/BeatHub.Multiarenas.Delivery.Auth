package com.beathub.multiarenas.delivery.auth.service.business.impl;

import com.beathub.multiarenas.delivery.auth.dto.request.ActualizarUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.CrearUsuarioRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.UserProfileResponse;
import com.beathub.multiarenas.delivery.auth.entity.ArenaUsuario;
import com.beathub.multiarenas.delivery.auth.entity.Persona;
import com.beathub.multiarenas.delivery.auth.entity.Rol;
import com.beathub.multiarenas.delivery.auth.entity.RolArenaUsuario;
import com.beathub.multiarenas.delivery.auth.entity.Usuario;
import com.beathub.multiarenas.delivery.auth.exception.BusinessException;
import com.beathub.multiarenas.delivery.auth.exception.ResourceNotFoundException;
import com.beathub.multiarenas.delivery.auth.repository.ArenaUsuarioRepository;
import com.beathub.multiarenas.delivery.auth.repository.PersonaRepository;
import com.beathub.multiarenas.delivery.auth.repository.RolRepository;
import com.beathub.multiarenas.delivery.auth.repository.UsuarioRepository;
import com.beathub.multiarenas.delivery.auth.security.UserPrincipal;
import com.beathub.multiarenas.delivery.auth.service.business.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private ArenaUsuarioRepository arenaUsuarioRepository;

    @Mock
    private com.beathub.multiarenas.delivery.auth.repository.RunnerZonaRepository runnerZonaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUpSecurity() {
        UserPrincipal principal = UserPrincipal.builder()
                .usuarioId(1L)
                .username("superadmin")
                .email("superadmin@beathub.com")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                .arenaIds(List.of(1L, 2L))
                .build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Debe crear usuario exitosamente con rol RUNNER y arena")
    void crearUsuario_Exitoso() {
        CrearUsuarioRequest request = CrearUsuarioRequest.builder()
                .nombres("Carlos")
                .apellidos("Gómez")
                .tipoDocumentoId(1)
                .numeroDocumento("1020304050")
                .email("carlos.runner@daviarrena.com")
                .password("Secret123*")
                .arenaId(1L)
                .rolId(3L)
                .shopId(101L)
                .shopNombre("Barra Principal")
                .build();

        Rol rol = Rol.builder().id(3L).nombre("RUNNER").estadoId(1).build();

        when(personaRepository.existsByEmail("carlos.runner@daviarrena.com")).thenReturn(false);
        when(personaRepository.existsByTipoDocumentoIdAndNumeroDocumento(1, "1020304050")).thenReturn(false);
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
        when(rolRepository.findById(3L)).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode("Secret123*")).thenReturn("hashedPassword");

        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(20L);
            return u;
        });

        when(arenaUsuarioRepository.save(any(ArenaUsuario.class))).thenAnswer(invocation -> {
            ArenaUsuario au = invocation.getArgument(0);
            au.setId(30L);
            return au;
        });

        UserProfileResponse mockProfile = UserProfileResponse.builder()
                .usuarioId(20L)
                .email("carlos.runner@daviarrena.com")
                .nombres("Carlos")
                .apellidos("Gómez")
                .build();
        when(authService.mapearUsuarioAProfileResponse(any(Usuario.class))).thenReturn(mockProfile);

        UserProfileResponse response = usuarioService.crearUsuario(request);

        assertNotNull(response);
        assertEquals(20L, response.getUsuarioId());
        assertEquals("carlos.runner@daviarrena.com", response.getEmail());
        verify(passwordEncoder).encode("Secret123*");
        verify(personaRepository).save(any(Persona.class));
        verify(usuarioRepository).save(any(Usuario.class));
        verify(arenaUsuarioRepository).save(any(ArenaUsuario.class));
    }

    @Test
    @DisplayName("Debe lanzar BusinessException si el correo ya existe")
    void crearUsuario_CorreoDuplicado() {
        CrearUsuarioRequest request = CrearUsuarioRequest.builder()
                .nombres("Ana")
                .apellidos("López")
                .tipoDocumentoId(1)
                .numeroDocumento("99887766")
                .email("ana.lopez@daviarrena.com")
                .build();

        when(personaRepository.existsByEmail("ana.lopez@daviarrena.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> usuarioService.crearUsuario(request));
        verify(personaRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar BusinessException si el documento ya existe")
    void crearUsuario_DocumentoDuplicado() {
        CrearUsuarioRequest request = CrearUsuarioRequest.builder()
                .nombres("Ana")
                .apellidos("López")
                .tipoDocumentoId(1)
                .numeroDocumento("99887766")
                .email("ana.nueva@daviarrena.com")
                .build();

        when(personaRepository.existsByEmail("ana.nueva@daviarrena.com")).thenReturn(false);
        when(personaRepository.existsByTipoDocumentoIdAndNumeroDocumento(1, "99887766")).thenReturn(true);

        assertThrows(BusinessException.class, () -> usuarioService.crearUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el rol no existe")
    void crearUsuario_RolNoEncontrado() {
        CrearUsuarioRequest request = CrearUsuarioRequest.builder()
                .nombres("Ana")
                .apellidos("López")
                .tipoDocumentoId(1)
                .numeroDocumento("99887766")
                .email("ana.nueva@daviarrena.com")
                .rolId(999L)
                .build();

        when(personaRepository.existsByEmail("ana.nueva@daviarrena.com")).thenReturn(false);
        when(personaRepository.existsByTipoDocumentoIdAndNumeroDocumento(1, "99887766")).thenReturn(false);
        when(rolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    @DisplayName("Debe actualizar usuario y persona exitosamente")
    void actualizarUsuario_Exitoso() {
        Persona persona = Persona.builder()
                .id(10L)
                .nombres("Carlos")
                .apellidos("Gómez")
                .email("carlos@daviarrena.com")
                .tipoDocumentoId(1)
                .numeroDocumento("1020304050")
                .build();

        Usuario usuario = Usuario.builder()
                .id(20L)
                .persona(persona)
                .username("carlos.gomez")
                .estadoId(1)
                .build();

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authService.mapearUsuarioAProfileResponse(any(Usuario.class))).thenReturn(
                UserProfileResponse.builder().usuarioId(20L).nombres("Carlos Editado").build()
        );

        ActualizarUsuarioRequest request = ActualizarUsuarioRequest.builder()
                .nombres("Carlos Editado")
                .telefono("+573009998877")
                .build();

        UserProfileResponse response = usuarioService.actualizarUsuario(20L, request);

        assertNotNull(response);
        assertEquals("Carlos Editado", persona.getNombres());
        assertEquals("+573009998877", persona.getTelefono());
        verify(personaRepository).save(persona);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Debe lanzar BusinessException si un usuario con RUNNER intenta tener BARRA sin reemplazo")
    void actualizarUsuario_IncompatibilidadRunnerYBarra_SinReemplazo() {
        Persona persona = Persona.builder().id(10L).email("runner@daviarrena.com").build();
        Usuario usuario = Usuario.builder().id(20L).persona(persona).username("runner").build();

        Rol rolRunner = Rol.builder().id(3L).nombre("RUNNER").estadoId(1).build();
        Rol rolBarra = Rol.builder().id(6L).nombre("BARRA").estadoId(1).build();

        ArenaUsuario au = ArenaUsuario.builder().id(100L).arenaId(1L).usuario(usuario).build();
        RolArenaUsuario rauRunner = RolArenaUsuario.builder().id(1L).rol(rolRunner).arenaUsuario(au).arenaId(1L).build();
        au.getRoles().add(rauRunner);
        usuario.getArenas().add(au);

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(6L)).thenReturn(Optional.of(rolBarra));

        ActualizarUsuarioRequest request = ActualizarUsuarioRequest.builder()
                .arenaId(1L)
                .rolId(6L)
                .reemplazarRolOperativo(false)
                .build();

        assertThrows(BusinessException.class, () -> usuarioService.actualizarUsuario(20L, request));
    }

    @Test
    @DisplayName("Debe reemplazar RUNNER por BARRA si reemplazarRolOperativo es true")
    void actualizarUsuario_ReemplazarRunnerPorBarra_Exitoso() {
        Persona persona = Persona.builder().id(10L).email("runner@daviarrena.com").build();
        Usuario usuario = Usuario.builder().id(20L).persona(persona).username("runner").build();

        Rol rolRunner = Rol.builder().id(3L).nombre("RUNNER").estadoId(1).build();
        Rol rolBarra = Rol.builder().id(6L).nombre("BARRA").estadoId(1).build();

        ArenaUsuario au = ArenaUsuario.builder().id(100L).arenaId(1L).usuario(usuario).build();
        RolArenaUsuario rauRunner = RolArenaUsuario.builder().id(1L).rol(rolRunner).arenaUsuario(au).arenaId(1L).build();
        au.getRoles().add(rauRunner);
        usuario.getArenas().add(au);

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findById(6L)).thenReturn(Optional.of(rolBarra));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authService.mapearUsuarioAProfileResponse(any(Usuario.class))).thenReturn(
                UserProfileResponse.builder().usuarioId(20L).build()
        );

        ActualizarUsuarioRequest request = ActualizarUsuarioRequest.builder()
                .arenaId(1L)
                .rolId(6L)
                .shopId(101L)
                .shopNombre("Barra Sur")
                .reemplazarRolOperativo(true)
                .build();

        UserProfileResponse response = usuarioService.actualizarUsuario(20L, request);

        assertNotNull(response);
        assertEquals(101L, au.getShopId());
        assertEquals("Barra Sur", au.getShopNombre());
        // Debe tener BARRA y NO RUNNER
        assertEquals(1, au.getRoles().size());
        assertEquals("BARRA", au.getRoles().get(0).getRol().getNombre());
    }

    @Test
    @DisplayName("Debe cambiar estado de usuario y persona")
    void cambiarEstado_Exitoso() {
        Persona persona = Persona.builder().id(10L).estadoId(1).build();
        Usuario usuario = Usuario.builder().id(20L).persona(persona).estadoId(1).build();

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));

        usuarioService.cambiarEstado(20L, 2);

        assertEquals(2, usuario.getEstadoId());
        assertEquals(2, persona.getEstadoId());
        verify(usuarioRepository).save(usuario);
        verify(personaRepository).save(persona);
    }
}
