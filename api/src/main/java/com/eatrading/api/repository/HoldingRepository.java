package com.eatrading.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eatrading.api.objects.Holding;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
}
