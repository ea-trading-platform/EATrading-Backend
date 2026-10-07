package com.eatrading.api.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.eatrading.api.services.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT Authentication Filter
 * 
 * Runs on every request to:
 * 1. Extract JWT token from "Authorization: Bearer <token>" header
 * 2. Validate token signature and expiration
 * 3. Extract client ID from token claims
 * 4. Store client ID in request attributes for controllers to use
 * 5. Reject requests with invalid/expired tokens
 * 
 * BR-02 Implementation:
 * - Ensures every request has valid authentication
 * - Client ID available to controllers via request.getAttribute("clientId")
 * - Controllers must verify client can access requested resource
 * 
 * Zero Trust Pattern:
 * - Every request is validated at the boundary (this filter)
 * - Controllers perform secondary validation via
 * AuthService.canAccessResource()
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Skip filtering for public endpoints
            String requestPath = request.getRequestURI();
            if (isPublicEndpoint(requestPath)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extract Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isEmpty()) {
                logger.debug("Request missing Authorization header: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Missing authorization header\"}");
                return;
            }

            // Extract token from "Bearer <token>" format
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token == null) {
                logger.warn("Invalid Authorization header format: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid authorization header format\"}");
                return;
            }

            // Validate token signature and expiration
            if (!jwtUtil.isTokenValid(token)) {
                logger.warn("Invalid or expired token for request: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
                return;
            }

            // Verify token type is ACCESS (not REFRESH)
            String tokenType = jwtUtil.getTokenTypeFromToken(token);
            if (!"ACCESS".equals(tokenType)) {
                logger.warn("Invalid token type for request (expected ACCESS, got {}): {}",
                        tokenType, requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid token type\"}");
                return;
            }

            // Extract client ID from token
            UUID clientId = jwtUtil.getClientIdFromToken(token);
            if (clientId == null) {
                logger.warn("Could not extract client ID from token: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid token claims\"}");
                return;
            }

            // Store client ID in request attributes
            // Controllers can retrieve via: (UUID) request.getAttribute("clientId")
            request.setAttribute("clientId", clientId);

            logger.debug("Token validated for client: {}, request path: {}", clientId, requestPath);

            // Continue with filter chain
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error in JWT authentication filter: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try {
                response.getWriter().write("{\"error\": \"Authentication failed\"}");
            } catch (IOException ioException) {
                logger.error("Error writing response: {}", ioException.getMessage());
            }
        }
    }

    /**
     * Check if request path is a public endpoint (no authentication required)
     */
    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.startsWith("/api/v1/auth/") ||
                requestPath.startsWith("/swagger-ui") ||
                requestPath.startsWith("/v3/api-docs") ||
                requestPath.equals("/swagger-ui.html") ||
                requestPath.equals("/");
    }

    /**
     * Static helper method for controllers to retrieve authenticated client ID
     * 
     * Usage in controller:
     * UUID clientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
     * 
     * @param request HttpServletRequest from controller
     * @return UUID of authenticated client, or null if not authenticated
     */
    public static UUID getAuthenticatedClientId(HttpServletRequest request) {
        Object clientIdObj = request.getAttribute("clientId");
        if (clientIdObj instanceof UUID) {
            return (UUID) clientIdObj;
        }
        return null;
    }
}
