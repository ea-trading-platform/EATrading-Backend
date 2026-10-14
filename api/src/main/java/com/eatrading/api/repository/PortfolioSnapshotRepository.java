package com.eatrading.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eatrading.api.entities.PortfolioSnapshot;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, UUID> {

	List<PortfolioSnapshot> findByClientIdAndTimestampBetweenOrderByTimestampAsc(
			UUID clientId, Instant from, Instant to);

	boolean existsByClientIdAndTimestamp(UUID clientId, Instant timestamp);
}
