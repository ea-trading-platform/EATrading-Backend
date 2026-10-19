package com.eatrading.api.services;

import java.util.Optional;
import java.util.UUID;

import com.eatrading.api.entities.Client;
import com.eatrading.api.repository.ClientRepository;

public class ClientService {
    
    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Optional<Client> getClientById(UUID clientId) {
        return clientRepository.findById(clientId);
    }

    public Client updateClient(Client client) {
        return clientRepository.save(client);
    }

    public Optional<Client> getClientByEmail(String clientEmail) {
        return clientRepository.findByEmail(clientEmail);
    }
    
}
