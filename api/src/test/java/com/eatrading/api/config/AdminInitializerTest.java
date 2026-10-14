package com.eatrading.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.eatrading.api.entities.Client;
import com.eatrading.api.objects.Role;
import com.eatrading.api.repository.ClientRepository;

import static org.mockito.Mockito.mock;
import org.mockito.ArgumentCaptor;

class AdminInitializerTest {

    @Test
    void createsAndMarksConfiguredAdmin() throws Exception {
        ClientRepository repository = mock(ClientRepository.class);
        AdminInitializer initializer = new AdminInitializer(repository);
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@eatrading.com");
        ReflectionTestUtils.setField(initializer, "adminPassword", "password123");

        when(repository.findByEmail("admin@eatrading.com")).thenReturn(Optional.empty());
        when(repository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer.run();

        ArgumentCaptor<Client> savedAdminCaptor = ArgumentCaptor.forClass(Client.class);
        verify(repository).save(savedAdminCaptor.capture());
        Client savedAdmin = savedAdminCaptor.getValue();
        assertEquals(Role.ADMIN, savedAdmin.getRole());
    }

    @Test
    void doesNotCreateAdminWithoutPassword() throws Exception {
        ClientRepository repository = mock(ClientRepository.class);
        AdminInitializer initializer = new AdminInitializer(repository);
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@eatrading.com");
        ReflectionTestUtils.setField(initializer, "adminPassword", "");

        initializer.run();

        verify(repository, never()).findByEmail(any());
        verify(repository, never()).save(any());
    }
}