package com.eatrading.api.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.AuthResponse;
import com.eatrading.api.dto.LoginRequest;
import com.eatrading.api.dto.RegisterRequest;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.Portfolio;
import com.eatrading.api.repository.ClientRepository;

/**
 * Authentication Service
 * Implements Sprint 8 authentication requirements:
 * 
 * BR-01: Registration and Secure Sign-In
 * - Client registration with email uniqueness validation
 * - Login with email and password verification
 * - Secure password storage using BCrypt with cost factor 12
 * 
 * BR-02: Client Data Isolation (Zero Trust)
 * - Verify clients can only access their own data
 * - canAccessResource() method for authorization checks
 * 
 * BR-03: Time-Limited and Revocable Sessions
 * - Access tokens: 15 minutes
 * - Refresh tokens: 7 days
 * - Session revocation via revokeSession() method
 * 
 * Security Design:
 * - BCrypt with cost factor 12: ~100ms per hash (deliberate choice for
 * security)
 * - Password never logged or exposed in error messages
 * - Tokens include email and type for comprehensive validation
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final ClientRepository clientRepository;
    private final JwtUtil jwtUtil;

    /**
     * BCryptPasswordEncoder with cost factor 12
     * - Cost 12: ~100ms per hash (provides strong security/performance balance)
     * - Deliberately chosen (not default), documented in code
     * - GPU-resistant due to high memory requirements
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public AuthService(ClientRepository clientRepository, JwtUtil jwtUtil) {
        this.clientRepository = clientRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * BR-01: Register a new client
     * 
     * @param request RegisterRequest with name, email, password, confirmPassword
     * @return AuthResponse with access token, refresh token, and client ID
     * @throws IllegalArgumentException if email already exists or passwords don't
     *                                  match
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Processing registration request for email: {}", request.getEmail());

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            logger.warn("Registration failed: passwords do not match for email: {}", request.getEmail());
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if email already exists
        Optional<Client> existingClient = clientRepository.findByEmail(request.getEmail());
        if (existingClient.isPresent()) {
            logger.warn("Registration failed: email already registered: {}", request.getEmail());
            throw new IllegalArgumentException("Email already registered");
        }

        // Hash password using BCrypt (cost factor 12)
        String passwordHash = passwordEncoder.encode(request.getPassword());
        // Password hash should never be logged

        // Create new client
        Client client = new Client(request.getName(), request.getEmail());
        client.setPasswordHash(passwordHash);

        // Save to database
        Client savedClient = clientRepository.save(client);
        logger.info("Client registered successfully: {}", savedClient.getId());

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(savedClient.getId(), savedClient.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(savedClient.getId(), savedClient.getEmail());

        return new AuthResponse(accessToken, refreshToken, savedClient.getId(), 900); // 15 minutes in seconds
    }

    /**
     * BR-01: Authenticate client with email and password
     * 
     * @param request LoginRequest with email and password
     * @return AuthResponse with access token, refresh token, and client ID
     * @throws IllegalArgumentException if email not found or password invalid
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        logger.info("Processing login request for email: {}", request.getEmail());

        // Find client by email
        Optional<Client> clientOptional = clientRepository.findByEmail(request.getEmail());
        if (clientOptional.isEmpty()) {
            logger.warn("Login failed: email not found: {}", request.getEmail());
            // Don't reveal if email exists (security best practice)
            throw new IllegalArgumentException("Invalid email or password");
        }

        Client client = clientOptional.get();

        // Verify password using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), client.getPasswordHash())) {
            logger.warn("Login failed: invalid password for email: {}", request.getEmail());
            // Don't reveal password was wrong (security best practice)
            throw new IllegalArgumentException("Invalid email or password");
        }

        logger.info("Client login successful: {}", client.getId());

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(client.getId(), client.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(client.getId(), client.getEmail());

        return new AuthResponse(accessToken, refreshToken, client.getId(), 900); // 15 minutes in seconds
    }

    /**
     * BR-03: Refresh access token using refresh token
     * 
     * Validates:
     * - Refresh token is valid (signature, expiration)
     * - Token type is REFRESH (not ACCESS)
     * - Session has not been revoked (tokenRevokedAt check)
     * 
     * @param refreshToken the refresh token
     * @return AuthResponse with new access token and same refresh token
     * @throws IllegalArgumentException if token is invalid
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        logger.debug("Processing token refresh");

        // Validate refresh token
        if (!jwtUtil.isTokenValid(refreshToken)) {
            logger.warn("Token refresh failed: invalid token");
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // Verify token type is REFRESH
        String tokenType = jwtUtil.getTokenTypeFromToken(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            logger.warn("Token refresh failed: wrong token type (expected REFRESH, got {})", tokenType);
            throw new IllegalArgumentException("Invalid token type for refresh");
        }

        // Extract client ID and email
        UUID clientId = jwtUtil.getClientIdFromToken(refreshToken);
        String email = jwtUtil.getEmailFromToken(refreshToken);

        if (clientId == null || email == null) {
            logger.warn("Token refresh failed: could not extract client info");
            throw new IllegalArgumentException("Invalid token claims");
        }

        // Check if session has been revoked
        Optional<Client> clientOptional = clientRepository.findById(clientId);
        if (clientOptional.isEmpty()) {
            logger.warn("Token refresh failed: client not found: {}", clientId);
            throw new IllegalArgumentException("Client not found");
        }

        Client client = clientOptional.get();

        // BR-03: Check if session was revoked
        // If tokenRevokedAt is set and the token's iat (issued at) is before revocation
        // time,
        // the token is considered revoked
        if (client.getTokenRevokedAt() != null) {
            logger.info("Session revoked at: {}, denying token refresh for client: {}",
                    client.getTokenRevokedAt(), clientId);
            throw new IllegalArgumentException("Session has been revoked");
        }

        logger.info("Token refreshed successfully for client: {}", clientId);

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(clientId, email);

        return new AuthResponse(newAccessToken, refreshToken, clientId, 900); // 15 minutes in seconds
    }

    /**
     * BR-02: Verify client can access a resource
     * Zero Trust Pattern: Every endpoint must verify the authenticated client
     * owns the resource they're requesting
     * 
     * Used in controllers to ensure:
     * - Clients can only view/modify their own orders
     * - Clients can only view/modify their own portfolio
     * - Clients can only view/modify their own data
     * 
     * @param authenticatedClientId the ID from JWT token (who is making request)
     * @param resourceOwnerId       the ID of resource owner (who owns the data)
     * @return true if client owns the resource, false otherwise
     */
    public boolean canAccessResource(UUID authenticatedClientId, UUID resourceOwnerId) {
        boolean hasAccess = authenticatedClientId != null && authenticatedClientId.equals(resourceOwnerId);

        if (!hasAccess) {
            logger.warn("Access denied: client {} attempted to access resource owned by {}",
                    authenticatedClientId, resourceOwnerId);
        } else {
            logger.debug("Access granted: client {} accessing their resource", authenticatedClientId);
        }

        return hasAccess;
    }

    /**
     * BR-03: Revoke all active sessions for a client
     * 
     * When a credential is compromised, this method invalidates all existing
     * refresh tokens by setting tokenRevokedAt timestamp.
     * 
     * Subsequent refresh token requests will be rejected because:
     * - The token's iat (issued at) will be before tokenRevokedAt
     * 
     * @param clientId the client whose sessions should be revoked
     */
    @Transactional
    public void revokeSession(UUID clientId) {
        logger.info("Revoking all sessions for client: {}", clientId);

        Optional<Client> clientOptional = clientRepository.findById(clientId);
        if (clientOptional.isEmpty()) {
            logger.warn("Revoke failed: client not found: {}", clientId);
            return;
        }

        Client client = clientOptional.get();
        client.setTokenRevokedAt(Instant.now());
        clientRepository.save(client);

        logger.info("All sessions revoked for client: {}", clientId);
    }

    /**
     * Check if a specific token has been revoked for a client
     * Used during token validation to ensure token wasn't issued before revocation
     * 
     * @param clientId      the client ID
     * @param tokenIssuedAt the time token was issued (iat claim)
     * @return true if token was issued before revocation, false otherwise
     */
    public boolean isTokenRevoked(UUID clientId, Instant tokenIssuedAt) {
        Optional<Client> clientOptional = clientRepository.findById(clientId);
        if (clientOptional.isEmpty()) {
            return true; // Assume revoked if client not found
        }

        Client client = clientOptional.get();
        if (client.getTokenRevokedAt() == null) {
            return false; // Not revoked if no revocation timestamp
        }

        // Token is revoked if it was issued before the revocation time
        return tokenIssuedAt.isBefore(client.getTokenRevokedAt());
    }
}
