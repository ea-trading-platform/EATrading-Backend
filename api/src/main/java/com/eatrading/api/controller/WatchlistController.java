package com.eatrading.api.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eatrading.api.config.JwtAuthenticationFilter;
import com.eatrading.api.dto.WatchlistRequest;
import com.eatrading.api.dto.WatchlistItemResponse;
import com.eatrading.api.services.WatchlistService;
import com.eatrading.api.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST Controller for Watchlist Management
 * 
 * Endpoints:
 * - GET /api/clients/watchlist - Get client's watchlist
 * - POST /api/clients/watchlist - Add stock to watchlist
 * - DELETE /api/clients/watchlist - Remove stock from watchlist
 * 
 * All endpoints implement BR-02 zero-trust validation:
 * - Authenticated client can only access/modify their own watchlist
 * - Server validates clientId in request matches authenticated client
 */
@RestController
@RequestMapping("/api/clients/watchlist")
public class WatchlistController {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistController.class);

    private final WatchlistService watchlistService;
    private final AuthService authService;

    public WatchlistController(WatchlistService watchlistService, AuthService authService) {
        this.watchlistService = watchlistService;
        this.authService = authService;
    }

    /**
     * GET /api/clients/watchlist?clientId={clientId}
     * 
     * Retrieve all stocks in a client's watchlist with current market data
     * 
     * BR-02: Verify authenticated client owns the watchlist
     * 
     * @param clientId UUID of the client whose watchlist to retrieve
     * @param request  HttpServletRequest for authentication context
     * @return List of WatchlistItemResponse with current prices (200 OK)
     * @throws 401 Unauthorized if missing/invalid JWT token
     * @throws 403 Forbidden if client trying to access another user's watchlist
     * @throws 404 Not Found if client doesn't exist
     */
    @GetMapping
    public ResponseEntity<?> getWatchlist(
            @RequestParam(name = "clientId", required = false) String clientId,
            @RequestParam(name = "client_id", required = false) String clientIdSnake,
            HttpServletRequest request) {

        try {
            // Get authenticated client ID from JWT filter
            UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
            UUID requestedClientId = resolveRequestedClientId(clientId, clientIdSnake);

            // BR-02: Zero Trust - Verify client can access their own watchlist
            if (!authService.canAccessResource(authenticatedClientId, requestedClientId)) {
                logger.warn("Unauthorized watchlist access attempt: client {} tried to access watchlist for {}",
                        authenticatedClientId, requestedClientId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            logger.info("Retrieving watchlist for client: {}", requestedClientId);
            List<WatchlistItemResponse> watchlist = watchlistService.getWatchlist(requestedClientId);
            return ResponseEntity.ok(watchlist);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid clientId format: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving watchlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/clients/watchlist
     * 
     * Add a stock symbol to a client's watchlist
     * 
     * BR-02: Verify authenticated client owns the watchlist being modified
     * 
     * Request body:
     * {
     * "clientId": "550e8400-e29b-41d4-a716-446655440000",
     * "symbol": "MSFT"
     * }
     * 
     * @param watchlistRequest Request containing clientId and symbol
     * @param request          HttpServletRequest for authentication context
     * @return WatchlistItemResponse with current price data (201 Created)
     * @throws 400 Bad Request if missing required fields
     * @throws 401 Unauthorized if missing/invalid JWT token
     * @throws 403 Forbidden if client trying to modify another user's watchlist
     * @throws 404 Not Found if client or symbol not found
     * @throws 409 Conflict if stock already in watchlist
     */
    @PostMapping
    public ResponseEntity<WatchlistItemResponse> addToWatchlist(
            @Valid @RequestBody WatchlistRequest watchlistRequest,
            HttpServletRequest request) {

        try {
            // Get authenticated client ID from JWT filter
            UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
            UUID requestedClientId = watchlistRequest.getClientId();
            String symbol = watchlistRequest.getSymbol();

            logger.debug("POST /api/clients/watchlist - authenticatedClientId: {}, requestedClientId: {}, symbol: {}",
                    authenticatedClientId, requestedClientId, symbol);

            // BR-02: Zero Trust - Verify client can modify their own watchlist
            if (authenticatedClientId == null) {
                logger.warn("Authenticated client ID is null for watchlist POST request");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!authService.canAccessResource(authenticatedClientId, requestedClientId)) {
                logger.warn("Unauthorized watchlist modification attempt: client {} tried to modify watchlist for {}",
                        authenticatedClientId, requestedClientId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            logger.info("Adding symbol {} to watchlist for client: {}", symbol, requestedClientId);
            WatchlistItemResponse response = watchlistService.addToWatchlist(requestedClientId, symbol);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (com.eatrading.api.exception.ConflictException e) {
            logger.warn("Conflict when adding to watchlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (com.eatrading.api.exception.ResourceNotFoundException e) {
            logger.warn("Resource not found when adding to watchlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request format: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error adding to watchlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/clients/watchlist?clientId={clientId}&symbol={symbol}
     * 
     * Remove a stock symbol from a client's watchlist
     * 
     * BR-02: Verify authenticated client owns the watchlist being modified
     * 
     * @param clientId UUID of the client
     * @param symbol   Stock symbol to remove
     * @param request  HttpServletRequest for authentication context
     * @return Empty response (200 OK on success)
     * @throws 400 Bad Request if missing required parameters
     * @throws 401 Unauthorized if missing/invalid JWT token
     * @throws 403 Forbidden if client trying to modify another user's watchlist
     * @throws 404 Not Found if client doesn't exist or stock not in watchlist
     */
    @DeleteMapping
    public ResponseEntity<?> removeFromWatchlist(
            @RequestParam(name = "clientId", required = false) String clientId,
            @RequestParam(name = "client_id", required = false) String clientIdSnake,
            @RequestParam String symbol,
            HttpServletRequest request) {

        try {
            // Get authenticated client ID from JWT filter
            UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
            UUID requestedClientId = resolveRequestedClientId(clientId, clientIdSnake);

            // BR-02: Zero Trust - Verify client can modify their own watchlist
            if (!authService.canAccessResource(authenticatedClientId, requestedClientId)) {
                logger.warn("Unauthorized watchlist modification attempt: client {} tried to modify watchlist for {}",
                        authenticatedClientId, requestedClientId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            logger.info("Removing symbol {} from watchlist for client: {}", symbol, requestedClientId);
            watchlistService.removeFromWatchlist(requestedClientId, symbol);
            return ResponseEntity.ok().build();

        } catch (com.eatrading.api.exception.ResourceNotFoundException e) {
            logger.warn("Resource not found when removing from watchlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request format: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error removing from watchlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private UUID resolveRequestedClientId(String clientId, String clientIdSnake) {
        String requestedClientId = clientId != null && !clientId.isBlank() ? clientId : clientIdSnake;
        if (requestedClientId == null || requestedClientId.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: clientId or client_id");
        }
        return UUID.fromString(requestedClientId);
    }
}
