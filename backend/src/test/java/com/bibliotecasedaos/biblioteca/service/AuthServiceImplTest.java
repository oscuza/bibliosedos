package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.config.JwtService;
import com.bibliotecasedaos.biblioteca.controller.models.AuthResponse;
import com.bibliotecasedaos.biblioteca.controller.models.AuthenticationRequest;
import com.bibliotecasedaos.biblioteca.controller.models.RegisterRequest;
import com.bibliotecasedaos.biblioteca.entity.Usuari;
import com.bibliotecasedaos.biblioteca.error.NickAlreadyExistsException;
import com.bibliotecasedaos.biblioteca.repository.UsuariRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaris per a la classe {@link AuthServiceImpl}.
 * 
 * @author David García Rodríguez
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuariRepository usuariRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private AuthenticationRequest authenticationRequest;
    private Usuari usuari;
    private String encodedPassword;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        encodedPassword = "$2a$10$encodedPassword";
        jwtToken = "test-jwt-token";

        registerRequest = RegisterRequest.builder()
                .nick("testuser")
                .nif("12345678A")
                .nom("Test")
                .cognom1("User")
                .cognom2("Test")
                .localitat("Barcelona")
                .provincia("Barcelona")
                .carrer("Carrer Test")
                .cp("08001")
                .tlf("123456789")
                .email("test@example.com")
                .password("password123")
                .rol(1)
                .build();

        authenticationRequest = AuthenticationRequest.builder()
                .nick("testuser")
                .password("password123")
                .build();

        usuari = Usuari.builder()
                .id(1L)
                .nick("testuser")
                .nif("12345678A")
                .nom("Test")
                .cognom1("User")
                .cognom2("Test")
                .localitat("Barcelona")
                .provincia("Barcelona")
                .carrer("Carrer Test")
                .cp("08001")
                .tlf("123456789")
                .email("test@example.com")
                .password(encodedPassword)
                .rol(1)
                .build();
    }

    @Test
    void testRegister_Success() {
        // Given
        when(usuariRepository.existsByNick(registerRequest.getNick())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
        when(usuariRepository.save(any(Usuari.class))).thenReturn(usuari);
        when(jwtService.generateToken(any(Usuari.class))).thenReturn(jwtToken);

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals(jwtToken, response.getToken());
        assertEquals(usuari.getId(), response.getId());
        assertEquals(usuari.getNom(), response.getNom());
        assertEquals(usuari.getCognom1(), response.getCognom1());
        assertEquals(usuari.getRol(), response.getRol());

        verify(usuariRepository).existsByNick(registerRequest.getNick());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(usuariRepository).save(any(Usuari.class));
        verify(jwtService).generateToken(any(Usuari.class));
    }

    @Test
    void testRegister_NickAlreadyExists_ThrowsException() {
        // Given
        when(usuariRepository.existsByNick(registerRequest.getNick())).thenReturn(true);

        // When & Then
        assertThrows(NickAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });

        verify(usuariRepository).existsByNick(registerRequest.getNick());
        verify(usuariRepository, never()).save(any(Usuari.class));
        verify(jwtService, never()).generateToken(any(Usuari.class));
    }

    @Test
    void testAuthenticate_Success() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(usuariRepository.findUsuariByNickWithJPQL(authenticationRequest.getNick()))
                .thenReturn(Optional.of(usuari));
        when(jwtService.generateToken(usuari)).thenReturn(jwtToken);

        // When
        AuthResponse response = authService.authenticate(authenticationRequest);

        // Then
        assertNotNull(response);
        assertEquals(jwtToken, response.getToken());
        assertEquals(usuari.getId(), response.getId());
        assertEquals(usuari.getNom(), response.getNom());
        assertEquals(usuari.getCognom1(), response.getCognom1());
        assertEquals(usuari.getRol(), response.getRol());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(usuariRepository).findUsuariByNickWithJPQL(authenticationRequest.getNick());
        verify(jwtService).generateToken(usuari);
    }

    @Test
    void testAuthenticate_InvalidCredentials_ThrowsException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Bad credentials") {});

        // When & Then
        assertThrows(org.springframework.security.core.AuthenticationException.class, () -> {
            authService.authenticate(authenticationRequest);
        });

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(usuariRepository, never()).findUsuariByNickWithJPQL(anyString());
        verify(jwtService, never()).generateToken(any(Usuari.class));
    }
}















