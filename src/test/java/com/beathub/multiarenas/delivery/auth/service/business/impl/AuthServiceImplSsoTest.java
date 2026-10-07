package com.beathub.multiarenas.delivery.auth.service.business.impl;

import com.beathub.multiarenas.delivery.auth.config.JwtProperties;
import com.beathub.multiarenas.delivery.auth.dto.request.ClientType;
import com.beathub.multiarenas.delivery.auth.dto.request.SsoLoginRequest;
import com.beathub.multiarenas.delivery.auth.dto.response.AuthResponse;
import com.beathub.multiarenas.delivery.auth.dto.sso.AzureTokenClaims;
import com.beathub.multiarenas.delivery.auth.entity.ArenaUsuario;
import com.beathub.multiarenas.delivery.auth.entity.Persona;
import com.beathub.multiarenas.delivery.auth.entity.Rol;
import com.beathub.multiarenas.delivery.auth.entity.Usuario;
import com.beathub.multiarenas.delivery.auth.exception.UnauthorizedException;
import com.beathub.multiarenas.delivery.auth.repository.ArenaUsuarioRepository;
import com.beathub.multiarenas.delivery.auth.repository.PersonaRepository;
import com.beathub.multiarenas.delivery.auth.repository.RolRepository;
import com.beathub.multiarenas.delivery.auth.repository.UsuarioRepository;
import com.beathub.multiarenas.delivery.auth.security.JwtTokenProvider;
import com.beathub.multiarenas.delivery.auth.service.client.AdsSsoClient;
import com.beathub.multiarenas.delivery.auth.service.client.AzureSsoValidatorService;
import com.beathub.multiarenas.delivery.auth.service.log.AppLoggerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplSsoTest {

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

    private SsoLoginRequest request;
    private AzureTokenClaims azureClaims;

    @BeforeEach
    void setUp() {
        request = new SsoLoginRequest();
        request.setSsoId("TBL-AZURE-GUID-123");
        request.setEmail("fan@tuboleta.com");
        request.setSsoToken("valid.jwt.token");
        request.setClientType(ClientType.USER_APP);
        request.setArenaId(1L);

        azureClaims = AzureTokenClaims.builder()
                .subject("TBL-AZURE-GUID-123")
                .email("fan@tuboleta.com")
                .name("Carlos Gomez")
                .givenName("Carlos")
                .familyName("Gomez")
                .build();
    }

    @Test
    void ssoLogin_SinSsoToken_LanzaUnauthorizedException() {
        request.setSsoToken(null);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                authService.ssoLogin(request)
        );
        assertTrue(ex.getMessage().contains("obligatorio"));
    }

    @Test
    void ssoLogin_TokenInvalidoORechazado_LanzaUnauthorizedException() {
        when(azureSsoValidatorService.validarToken(eq("invalid.jwt.token"), any(), any()))
                .thenThrow(new UnauthorizedException("La firma criptográfica del token de SSO de Azure es inválida"));

        request.setSsoToken("invalid.jwt.token");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                authService.ssoLogin(request)
        );
        assertTrue(ex.getMessage().contains("inválida"));
        verify(appLoggerService).logIntegrationService(any(), any(), any(), any(), any(), any(), any(), any(), eq(401), eq("ERROR"), eq(1));
    }

    @Test
    void ssoLogin_UsuarioExistente_RetornaTokenExitoso() {
        when(azureSsoValidatorService.validarToken(eq("valid.jwt.token"), eq("TBL-AZURE-GUID-123"), eq("fan@tuboleta.com")))
                .thenReturn(azureClaims);

        Persona persona = Persona.builder().id(10L).email("fan@tuboleta.com").build();
        Usuario usuario = Usuario.builder()
                .id(1L)
                .username("carlos.gomez")
                .ssoId("TBL-AZURE-GUID-123")
                .persona(persona)
                .estadoId(1)
                .arenas(new ArrayList<>())
                .build();

        when(usuarioRepository.findBySsoId("TBL-AZURE-GUID-123")).thenReturn(Optional.of(usuario));
        when(jwtProperties.getExpirationMs()).thenReturn(86400000L);
        when(jwtTokenProvider.generarToken(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("beathub-jwt-access-token");

        Rol rolCliente = Rol.builder().id(4L).nombre("CLIENTE").build();
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(arenaUsuarioRepository.save(any(ArenaUsuario.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.ssoLogin(request);

        assertNotNull(response);
        assertEquals("beathub-jwt-access-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400000L, response.getExpiresIn());

        verify(appLoggerService).logIntegrationService(any(), any(), any(), any(), any(), any(), any(), any(), eq(200), eq("SUCCESS"), eq(1));
    }

    @Test
    void ssoLogin_UsuarioNuevo_AutoAprovisionaJitYRetornaTokenExitoso() {
        when(azureSsoValidatorService.validarToken(eq("valid.jwt.token"), eq("TBL-AZURE-GUID-123"), eq("fan@tuboleta.com")))
                .thenReturn(azureClaims);

        when(usuarioRepository.findBySsoId("TBL-AZURE-GUID-123")).thenReturn(Optional.empty());
        when(usuarioRepository.findByPersonaEmail("fan@tuboleta.com")).thenReturn(Optional.empty());
        when(personaRepository.findByEmail("fan@tuboleta.com")).thenReturn(Optional.empty());
        when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> {
            Persona p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });
        when(usuarioRepository.existsByUsername(any())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });

        Rol rolCliente = Rol.builder().id(4L).nombre("CLIENTE").build();
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(arenaUsuarioRepository.save(any(ArenaUsuario.class))).thenAnswer(inv -> inv.getArgument(0));

        when(jwtProperties.getExpirationMs()).thenReturn(86400000L);
        when(jwtTokenProvider.generarToken(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("beathub-jwt-access-token-new-user");

        AuthResponse response = authService.ssoLogin(request);

        assertNotNull(response);
        assertEquals("beathub-jwt-access-token-new-user", response.getToken());
        verify(personaRepository).save(argThat(p ->
                "Carlos".equals(p.getNombres()) &&
                "Gomez".equals(p.getApellidos()) &&
                "fan@tuboleta.com".equals(p.getEmail())
        ));
    }
}
