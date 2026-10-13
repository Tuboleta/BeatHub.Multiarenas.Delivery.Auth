package com.beathub.multiarenas.delivery.auth.service.business.impl;

import com.beathub.multiarenas.delivery.auth.config.JwtProperties;
import com.beathub.multiarenas.delivery.auth.dto.request.ClientType;
import com.beathub.multiarenas.delivery.auth.dto.request.LoginRequest;
import com.beathub.multiarenas.delivery.auth.dto.request.RefreshTokenRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.AuthResponse;
import com.beathub.multiarenas.delivery.auth.entity.ArenaUsuario;
import com.beathub.multiarenas.delivery.auth.entity.Persona;
import com.beathub.multiarenas.delivery.auth.entity.Rol;
import com.beathub.multiarenas.delivery.auth.entity.RolArenaUsuario;
import com.beathub.multiarenas.delivery.auth.entity.Usuario;
import com.beathub.multiarenas.delivery.auth.exception.BadRequestException;
import com.beathub.multiarenas.delivery.auth.exception.ForbiddenException;
import com.beathub.multiarenas.delivery.auth.exception.UnauthorizedException;
import com.beathub.multiarenas.delivery.auth.repository.ArenaUsuarioRepository;
import com.beathub.multiarenas.delivery.auth.repository.PersonaRepository;
import com.beathub.multiarenas.delivery.auth.repository.RolRepository;
import com.beathub.multiarenas.delivery.auth.repository.UsuarioRepository;
import com.beathub.multiarenas.delivery.auth.security.JwtTokenProvider;
import com.beathub.multiarenas.delivery.auth.service.client.AdsSsoClient;
import com.beathub.multiarenas.delivery.auth.service.client.AzureSsoValidatorService;
import com.beathub.multiarenas.delivery.auth.service.log.AppLoggerService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplBackOfficeTest {

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private ArenaUsuarioRepository arenaUsuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AdsSsoClient adsSsoClient;

    @Mock
    private AzureSsoValidatorService azureSsoValidatorService;

    @Mock
    private AppLoggerService appLoggerService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario backofficeUser;
    private Persona backofficePersona;

    @BeforeEach
    void setUp() {
        backofficePersona = Persona.builder()
                .id(2L)
                .nombres("BackOffice")
                .apellidos("System")
                .email("backoffice@beathub.com")
                .numeroDocumento("9000000002")
                .estadoId(1)
                .build();

        Rol rolBackoffice = Rol.builder()
                .id(5L)
                .nombre("BACKOFFICE")
                .descripcion("Rol de integración BackOffice")
                .estadoId(1)
                .build();

        ArenaUsuario au1 = ArenaUsuario.builder()
                .id(3L)
                .arenaId(1L)
                .estadoId(1)
                .roles(new ArrayList<>())
                .build();

        RolArenaUsuario rau1 = RolArenaUsuario.builder()
                .id(3L)
                .rol(rolBackoffice)
                .arenaUsuario(au1)
                .arenaId(1L)
                .estadoId(1)
                .build();
        au1.getRoles().add(rau1);

        backofficeUser = Usuario.builder()
                .id(2L)
                .persona(backofficePersona)
                .username("backoffice")
                .password("$2a$10$hashedPassword")
                .estadoId(1)
                .arenas(new ArrayList<>(List.of(au1)))
                .build();
        au1.setUsuario(backofficeUser);
    }

    @Test
    void login_BackOffice_CredencialesValidas_RetornaTokens() {
        LoginRequest request = LoginRequest.builder()
                .login("backoffice")
                .password("Admin12345!")
                .clientType(ClientType.BACKOFFICE)
                .arenaId(1L)
                .build();

        when(usuarioRepository.findByUsernameOrEmail("backoffice")).thenReturn(Optional.of(backofficeUser));
        when(passwordEncoder.matches("Admin12345!", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtProperties.getExpirationMs()).thenReturn(86400000L);
        when(jwtTokenProvider.generarToken(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("mock.jwt.token");
        when(jwtTokenProvider.generarRefreshToken(eq(2L), eq("backoffice"), eq(1L), any()))
                .thenReturn("mock.refresh.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("mock.refresh.token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400000L, response.getExpiresIn());
    }

    @Test
    void login_BackOffice_PasswordInvalido_LanzaUnauthorizedException() {
        LoginRequest request = LoginRequest.builder()
                .login("backoffice")
                .password("WrongPassword")
                .clientType(ClientType.BACKOFFICE)
                .build();

        when(usuarioRepository.findByUsernameOrEmail("backoffice")).thenReturn(Optional.of(backofficeUser));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                authService.login(request)
        );
        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    @Test
    void login_BackOffice_UsuarioSinRolBackOffice_LanzaForbiddenException() {
        // Usuario con rol CLIENTE únicamente
        Rol rolCliente = Rol.builder().id(4L).nombre("CLIENTE").build();
        ArenaUsuario auCliente = ArenaUsuario.builder()
                .id(10L)
                .arenaId(1L)
                .roles(new ArrayList<>(List.of(
                        RolArenaUsuario.builder().rol(rolCliente).build()
                )))
                .build();

        Usuario clienteUser = Usuario.builder()
                .id(50L)
                .persona(backofficePersona)
                .username("cliente.comun")
                .password("$2a$10$hashedPassword")
                .estadoId(1)
                .arenas(List.of(auCliente))
                .build();

        LoginRequest request = LoginRequest.builder()
                .login("cliente.comun")
                .password("Admin12345!")
                .clientType(ClientType.BACKOFFICE)
                .build();

        when(usuarioRepository.findByUsernameOrEmail("cliente.comun")).thenReturn(Optional.of(clienteUser));
        when(passwordEncoder.matches("Admin12345!", "$2a$10$hashedPassword")).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                authService.login(request)
        );
        assertTrue(ex.getMessage().contains("BackOffice"));
    }

    @Test
    void login_BackOffice_ClientTypeNoPermitido_LanzaBadRequestException() {
        LoginRequest request = LoginRequest.builder()
                .login("backoffice")
                .password("Admin12345!")
                .clientType(ClientType.USER_APP)
                .build();

        when(usuarioRepository.findByUsernameOrEmail("backoffice")).thenReturn(Optional.of(backofficeUser));
        when(passwordEncoder.matches("Admin12345!", "$2a$10$hashedPassword")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                authService.login(request)
        );
        assertTrue(ex.getMessage().contains("BACKOFFICE"));
    }

    @Test
    void refreshTokenConRefreshToken_TokenValido_RetornaNuevosTokens() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("mock.valid.refresh.token")
                .arenaId(1L)
                .build();

        Claims mockClaims = mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("2");

        when(jwtTokenProvider.validarRefreshToken("mock.valid.refresh.token")).thenReturn(true);
        when(jwtTokenProvider.extraerClaims("mock.valid.refresh.token")).thenReturn(mockClaims);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(backofficeUser));
        when(jwtProperties.getExpirationMs()).thenReturn(86400000L);
        when(jwtTokenProvider.generarToken(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("new.access.token");
        when(jwtTokenProvider.generarRefreshToken(eq(2L), eq("backoffice"), eq(1L), any()))
                .thenReturn("new.refresh.token");

        AuthResponse response = authService.refreshTokenConRefreshToken(request);

        assertNotNull(response);
        assertEquals("new.access.token", response.getToken());
        assertEquals("new.refresh.token", response.getRefreshToken());
    }

    @Test
    void refreshTokenConRefreshToken_TokenExpiradoOInvalido_LanzaUnauthorizedException() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expired.refresh.token")
                .build();

        when(jwtTokenProvider.validarRefreshToken("expired.refresh.token")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                authService.refreshTokenConRefreshToken(request)
        );
        assertTrue(ex.getMessage().contains("inválido o ha expirado"));
    }
}
