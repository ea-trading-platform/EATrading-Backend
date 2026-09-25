package com.eatrading.api.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eatrading.api.Objects.Holding;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
}
