package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.eatrading.api.objects.Role;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-that-is-at-least-32-bytes-long");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 604800000L);
        clientId = UUID.randomUUID();
    }

    @Test
    void accessTokenContainsRoleAndAccessType() {
        String token = jwtUtil.generateAccessToken(clientId, "admin@eatrading.com", Role.ADMIN);

        assertEquals(clientId, jwtUtil.getClientIdFromToken(token));
        assertEquals("admin@eatrading.com", jwtUtil.getEmailFromToken(token));
        assertEquals(Role.ADMIN, jwtUtil.getRoleFromToken(token));
        assertEquals("ACCESS", jwtUtil.getTokenTypeFromToken(token));
    }

    @Test
    void refreshTokenContainsClientRoleAndRefreshType() {
        String token = jwtUtil.generateRefreshToken(clientId, "client@example.com", Role.CLIENT);

        assertEquals(Role.CLIENT, jwtUtil.getRoleFromToken(token));
        assertEquals("REFRESH", jwtUtil.getTokenTypeFromToken(token));
    }

    @Test
    void tokenWithoutRoleReturnsNullRole() {
        String token = jwtUtil.generateAccessToken(clientId, "client@example.com");

        assertEquals(Role.CLIENT, jwtUtil.getRoleFromToken(token));
        assertNull(jwtUtil.getRoleFromToken("not-a-token"));
    }
}