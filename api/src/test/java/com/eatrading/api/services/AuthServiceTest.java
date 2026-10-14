package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.eatrading.api.dto.AuthResponse;
import com.eatrading.api.dto.LoginRequest;
import com.eatrading.api.dto.RegisterRequest;
import com.eatrading.api.entities.Client;
import com.eatrading.api.objects.Role;
import com.eatrading.api.repository.ClientRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(clientRepository, jwtUtil);
    }

    @Test
    void registrationCreatesClientRoleAndReturnsRole() {
        Client savedClient = new Client("Jane Smith", "jane@example.com");
        savedClient.setRole(Role.CLIENT);
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        when(clientRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn(accessToken);
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn(refreshToken);

        AuthResponse response = authService.register(new RegisterRequest(
                "Jane Smith", "jane@example.com", "password123", "password123"));

        assertEquals(Role.CLIENT, response.getRole());
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void registrationRejectsReservedAdministratorEmail() {
        RegisterRequest request = new RegisterRequest(
                "Impostor", " ADMIN@EATRADING.COM ", "password123", "password123");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void loginReturnsPersistedAdminRole() {
        Client admin = new Client("EA Trading Administrator", "admin@eatrading.com");
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(new BCryptPasswordEncoder(12).encode("password"));

        when(clientRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest(admin.getEmail(), "password"));

        assertEquals(Role.ADMIN, response.getRole());
    }
}