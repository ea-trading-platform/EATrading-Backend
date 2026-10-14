package com.eatrading.api.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name= "portfolio_snapshot" )
public class PortfolioSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private UUID clientId;

    @Column(unique = true, nullable = false)
    private Instant timestamp;

    @Column(unique = true, nullable = false)
    private BigDecimal portfolioValueUsd;

    @Column(unique = true, nullable = false)
    private BigDecimal cashValueUsd;

    @Column(unique = true, nullable = false)
    private BigDecimal invested


}
