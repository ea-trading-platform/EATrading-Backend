package com.eatrading.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eatrading.api.dto.AuthResponse;
import com.eatrading.api.dto.LoginRequest;
import com.eatrading.api.dto.RegisterRequest;
import com.eatrading.api.dto.TokenRefreshRequest;
import com.eatrading.api.services.AuthService;

import jakarta.validation.Valid;

/**
 * Authentication Controller
 * 
 * Provides REST endpoints for client authentication:
 * - POST /api/v1/auth/register - Register new client (BR-01)
 * - POST /api/v1/auth/login - Authenticate client (BR-01)
 * - POST /api/v1/auth/refresh - Refresh access token (BR-03)
 * - GET /api/v1/auth/health - Health check
 * 
 * All endpoints are public (no authentication required)
 * Responses include JWT tokens for subsequent API requests
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * BR-01: Register a new client
     * 
     * POST /api/v1/auth/register
     * 
     * @param request RegisterRequest with name, email, password, confirmPassword
     * @return AuthResponse with tokens (201 Created)
     * @throws IllegalArgumentException if email exists or passwords don't match
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Register endpoint called for email: {}", request.getEmail());

        try {
            AuthResponse response = authService.register(request);
            logger.info("Registration successful for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Unexpected error during registration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * BR-01: Authenticate client with email and password
     * 
     * POST /api/v1/auth/login
     * 
     * Returns:
     * {
     * "accessToken": "eyJhbGc...",
     * "refreshToken": "eyJhbGc...",
     * "clientId": "550e8400-e29b-41d4-a716-446655440000",
     * "expiresIn": 900,
     * "tokenType": "Bearer"
     * }
     * 
     * @param request LoginRequest with email and password
     * @return AuthResponse with tokens (200 OK)
     * @throws IllegalArgumentException if email not found or password invalid
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login endpoint called for email: {}", request.getEmail());

        try {
            AuthResponse response = authService.login(request);
            logger.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.error("Unexpected error during login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * BR-03: Refresh access token
     * 
     * POST /api/v1/auth/refresh
     * 
     * Takes a valid refresh token and returns a new access token.
     * Refresh token remains the same and is still valid.
     * 
     * Validation:
     * - Refresh token must be valid (not expired, correct signature)
     * - Refresh token must have type=REFRESH
     * - Client's session must not have been revoked
     * 
     * @param request TokenRefreshRequest with refreshToken
     * @return AuthResponse with new access token
     * @throws IllegalArgumentException if token invalid or session revoked
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        logger.debug("Refresh endpoint called");

        try {
            AuthResponse response = authService.refreshToken(request.getRefreshToken());
            logger.debug("Token refresh successful");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.error("Unexpected error during token refresh: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     * 
     * GET /api/v1/auth/health
     * 
     * Simple endpoint to verify auth service is running
     * 
     * @return 200 OK with "UP" status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        logger.debug("Health check endpoint called");
        return ResponseEntity.ok("UP");
    }
}
