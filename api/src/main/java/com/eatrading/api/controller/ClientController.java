package com.eatrading.api.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eatrading.api.repository.ClientRepository;
import com.eatrading.api.dto.ClientUpdateRequest;
import com.eatrading.api.entities.Client;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository clientRepository;

    public ClientController(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * GET /api/clients
     * Get all clients
     */
    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        List<Client> clients = clientRepository.findAll();
        return ResponseEntity.ok(clients);
    }

    /**
     * GET /api/clients?clientId={clientId}
     * Get client by ID - Only accessible to the authenticated user for their own ID
     */
    @GetMapping(params = "clientId")
    public ResponseEntity<Client> getClientById(@RequestParam String clientId) {
        // Verify authorization - user can only access their own data
        if (!isAuthorized(clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Optional<Client> client = clientRepository.findById(UUID.fromString(clientId));
        
        if (!client.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(client.get());
    }

    /**
     * PUT /api/clients?clientId={clientId}
     * Update client - Only authenticated user can update their own profile
     * Accepts query parameters for name and email or request body with ClientUpdateRequest
     */
    @PutMapping
    public ResponseEntity<Client> updateClient(
            @RequestParam String clientId,
            @Valid @RequestBody ClientUpdateRequest request) {
        // Verify authorization - user can only update their own profile
        if (!isAuthorized(clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Optional<Client> clientOptional = clientRepository.findById(UUID.fromString(clientId));
        
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
        
        Client updatedClient = clientRepository.save(client);
        return ResponseEntity.ok(updatedClient);
    }
    
    /**
     * Helper method to verify user authorization
     * Returns true if the authenticated user's ID matches the requested clientId
     */
    private boolean isAuthorized(String clientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // Get the authenticated user's principal (assuming it's the user ID)
        String authenticatedUserId = authentication.getName();
        return authenticatedUserId.equals(clientId);
    }

}
