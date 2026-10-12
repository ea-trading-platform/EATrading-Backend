package com.eatrading.api.controller;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eatrading.api.config.JwtAuthenticationFilter;
import com.eatrading.api.dto.ClientResponse;
import com.eatrading.api.dto.ClientUpdateRequest;
import com.eatrading.api.entities.Client;
import com.eatrading.api.repository.ClientRepository;
import com.eatrading.api.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    private final ClientRepository clientRepository;
    private final AuthService authService;

    public ClientController(ClientRepository clientRepository, AuthService authService) {
        this.clientRepository = clientRepository;
        this.authService = authService;
    }

    /**
     * GET /api/clients
     * Bulk client listing is disabled.
     * 
     * Returning all client records exposes sensitive account data to any
     * authenticated caller.
     */
    @GetMapping
    public ResponseEntity<Void> getAllClients() {
        logger.warn("Blocked bulk client listing request");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * GET /api/clients?clientId={clientId}
     * Get client by ID
     * 
     * BR-02: Zero Trust - Verify authenticated client owns the requested data
     */
    @GetMapping(params = "clientId")
    public ResponseEntity<ClientResponse> getClientById(
            @RequestParam String clientId,
            HttpServletRequest request) {

        // Get authenticated client ID from JWT filter
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
        UUID requestedClientId = UUID.fromString(clientId);

        // BR-02: Verify client can access their own data
        if (!authService.canAccessResource(authenticatedClientId, requestedClientId)) {
            logger.warn("Unauthorized access attempt: client {} tried to access client profile for {}",
                    authenticatedClientId, requestedClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Client> client = clientRepository.findById(requestedClientId);

        if (!client.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ClientResponse.fromEntity(client.get()));
    }

    /**
     * PUT /api/clients?clientId={clientId}
     * Update client
     * 
     * BR-02: Zero Trust - Verify authenticated client owns the resource being
     * modified
     */
    @PutMapping
    public ResponseEntity<ClientResponse> updateClient(
            @RequestParam String clientId,
            @Valid @RequestBody ClientUpdateRequest request,
            HttpServletRequest httpRequest) {

        // Get authenticated client ID from JWT filter
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(httpRequest);
        UUID requestedClientId = UUID.fromString(clientId);

        // BR-02: Verify client can modify their own data
        if (!authService.canAccessResource(authenticatedClientId, requestedClientId)) {
            logger.warn("Unauthorized modification attempt: client {} tried to update client profile for {}",
                    authenticatedClientId, requestedClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Client> clientOptional = clientRepository.findById(requestedClientId);

        if (!clientOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Client client = clientOptional.get();

        if (request.getName() != null && !request.getName().isBlank()) {
            client.setName(request.getName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            client.setEmail(request.getEmail());
        }

        logger.info("Updated client: {}", requestedClientId);
        Client updatedClient = clientRepository.save(client);
        return ResponseEntity.ok(ClientResponse.fromEntity(updatedClient));
    }

}
