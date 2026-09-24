package com.eatrading.backend.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eatrading.backend.Objects.Holding;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
}
