package com.eatrading.api.services;

import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

/**
 * JWT Token Utility Service
 * Handles token generation, validation, and claim extraction
 * 
 * BR-03 Implementation:
 * - Access tokens: 15 minutes (900 seconds)
 * - Refresh tokens: 7 days (604800 seconds)
 * - Token type claim distinguishes access vs refresh tokens
 */
@Service
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:900000}")
    private long accessTokenExpiration; // 15 minutes in milliseconds

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration; // 7 days in milliseconds

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    /**
     * Generate access token for authenticated client
     * Token expires in 15 minutes
     */
    public String generateAccessToken(UUID clientId, String email) {
        return generateToken(clientId, email, TOKEN_TYPE_ACCESS, accessTokenExpiration);
    }

    /**
     * Generate refresh token for authenticated client
     * Token expires in 7 days
     * Refresh tokens can be used to obtain new access tokens
     */
    public String generateRefreshToken(UUID clientId, String email) {
        return generateToken(clientId, email, TOKEN_TYPE_REFRESH, refreshTokenExpiration);
    }

    /**
     * Internal method to generate JWT token with custom claims
     */
    private String generateToken(UUID clientId, String email, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationMs);

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

            String token = Jwts.builder()
                    .subject(clientId.toString()) // sub claim: client ID
                    .claim("email", email)
                    .claim("type", tokenType) // access or refresh
                    .issuedAt(now) // iat claim: issued at
                    .expiration(expirationDate) // exp claim: expiration
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            logger.debug("Generated {} token for client: {}", tokenType, clientId);
            return token;
        } catch (Exception e) {
            logger.error("Error generating {} token: {}", tokenType, e.getMessage());
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /**
     * Validate JWT token
     * Checks signature, expiration, and format
     */
    public boolean isTokenValid(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            logger.debug("Token validation successful");
            return true;
        } catch (JwtException e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error during token validation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract all claims from token
     */
    public Claims getAllClaimsFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.warn("Failed to extract claims: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    /**
     * Extract client ID from token subject claim
     * BR-02 Implementation: Used to verify client ownership of resources
     */
    public UUID getClientIdFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String clientId = claims.getSubject();
            if (clientId != null) {
                return UUID.fromString(clientId);
            }
            logger.warn("Client ID not found in token");
            return null;
        } catch (Exception e) {
            logger.error("Error extracting client ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract email from token claims
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            logger.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract token type (ACCESS or REFRESH) from claims
     * Used to ensure correct token type for operation
     */
    public String getTokenTypeFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.get("type", String.class);
        } catch (Exception e) {
            logger.error("Error extracting token type from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get token expiration date from claims
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            logger.error("Error extracting expiration date: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return true;
        }
        return expiration.before(new Date());
    }

    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
