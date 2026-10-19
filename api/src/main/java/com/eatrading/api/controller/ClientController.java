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
import com.eatrading.api.objects.Role;
import com.eatrading.api.services.AuthService;
import com.eatrading.api.services.ClientService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    // private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final AuthService authService;

    public ClientController(ClientService clientService, AuthService authService) {
        this.authService = authService;
        this.clientService = clientService;
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

        Optional<Client> client = clientService.getClientById(requestedClientId);
        
        if (client.isEmpty()) {
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

        Optional<Client> client = clientService.getClientById(requestedClientId);

        if (client.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Client existingClient = client.get();

        if (request.getName() != null && !request.getName().isBlank()) {
            existingClient.setName(request.getName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            existingClient.setEmail(request.getEmail());
        }

        logger.info("Updated client: {}", requestedClientId);
        Client updatedClient = clientService.updateClient(existingClient);
        return ResponseEntity.ok(ClientResponse.fromEntity(updatedClient));
    }

    /**
     * GET /api/clients?clientEmail={clientEmail}
     * Get client by email
     * 
     * BR-02: Zero Trust - Verify authenticated client owns the requested data.
     * Administrators may access any client profile.
     */
    @GetMapping(params = "clientEmail")
    public ResponseEntity<ClientResponse> getClientByEmail(
            @RequestParam String clientEmail,
            
            HttpServletRequest request) {

        // Get authenticated client ID from JWT filter
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
        String requestedClientEmail = clientEmail;

        // Administrators may access any client profile; clients may access only
        // the profile associated with their authenticated email.
        Role authenticatedRole = JwtAuthenticationFilter.getAuthenticatedRole(request);
        boolean isAdmin = authenticatedRole == Role.ADMIN;
        if (!isAdmin && !authService.canAccessResource(authenticatedClientId, requestedClientEmail)) {
            logger.warn("Unauthorized access attempt: client {} tried to access client profile for {}",
                    authenticatedClientId, requestedClientEmail);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Client> client = clientService.getClientByEmail(clientEmail);
        
        if (client.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ClientResponse.fromEntity(client.get()));
    }


}
