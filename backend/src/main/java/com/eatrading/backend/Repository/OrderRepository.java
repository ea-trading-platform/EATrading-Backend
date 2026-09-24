package com.eatrading.backend.Repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eatrading.backend.Objects.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByClientId(UUID clientId);
}
