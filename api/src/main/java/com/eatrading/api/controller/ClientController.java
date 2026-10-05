package com.eatrading.api.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
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
     * Get client by ID
     */
    @GetMapping(params = "clientId")
    public ResponseEntity<Client> getClientById(@RequestParam String clientId) {
        Optional<Client> client = clientRepository.findById(UUID.fromString(clientId));
        
        if (!client.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(client.get());
    }

    /**
     * PUT /api/clients?clientId={clientId}
     * Update client
     * Accepts query parameters for name and email or request body with ClientUpdateRequest
     */
    @PutMapping
    public ResponseEntity<Client> updateClient(
            @RequestParam String clientId,
            @Valid @RequestBody ClientUpdateRequest request) {
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

}
