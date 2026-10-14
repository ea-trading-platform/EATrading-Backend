package com.eatrading.api.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eatrading.api.config.JwtAuthenticationFilter;
import com.eatrading.api.dto.PortfolioSummaryResponse;
import com.eatrading.api.services.AuthService;
import com.eatrading.api.services.PortfolioService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);

    private final AuthService authService;
    private final PortfolioService portfolioService;

    public PortfolioController(AuthService authService, PortfolioService portfolioService) {
        this.authService = authService;
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummary(
            @RequestParam String clientId, HttpServletRequest request) {
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
        UUID requestedClientId = UUID.fromString(clientId);

        if (!authService.canAccessResource(authenticatedClientId, requestedClientId)) {
            logger.warn("Unauthorized portfolio access attempt: client {} tried to access {}",
                    authenticatedClientId, requestedClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(portfolioService.getSummary(requestedClientId));
    }

    @GetMapping("/history")
    public ResponseEntity<Void> getPortfolioHistory(
            @RequestParam String clientId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            HttpServletRequest request) {
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
        UUID requestedClientId = UUID.fromString(clientId);

        if (!authService.canAccessResource(authenticatedClientId, requestedClientId)) {
            logger.warn("Unauthorized portfolio history access attempt: client {} tried to access {}",
                    authenticatedClientId, requestedClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        logger.warn("Portfolio history requested for client {} from {} to {}, but snapshots are not implemented",
                requestedClientId, from, to);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}