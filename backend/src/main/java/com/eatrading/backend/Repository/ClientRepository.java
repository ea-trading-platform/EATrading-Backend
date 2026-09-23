package com.eatrading.backend.Repository;

import com.eatrading.backend.Objects.Client;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}