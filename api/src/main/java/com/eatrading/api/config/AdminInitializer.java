package com.eatrading.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.eatrading.api.entities.Client;
import com.eatrading.api.objects.Role;
import com.eatrading.api.repository.ClientRepository;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final ClientRepository clientRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    public AdminInitializer(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            logger.warn("ADMIN_PASSWORD is not configured; administrator account was not initialized");
            return;
        }

        Client admin = clientRepository.findByEmail(adminEmail).orElseGet(() -> {
            Client newAdmin = new Client("EA Trading Administrator", adminEmail);
            newAdmin.setPasswordHash(passwordEncoder.encode(adminPassword));
            return newAdmin;
        });

        admin.setRole(Role.ADMIN);
        clientRepository.save(admin);
        logger.info("Administrator account is available for {}", adminEmail);
    }
}