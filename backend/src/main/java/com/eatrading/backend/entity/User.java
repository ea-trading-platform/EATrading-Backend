package com.eatrading.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "total_balance")
    private Float totalBalance;

    public User() {
    }

    public User(UUID userId, String name, Float totalBalance) {
        this.userId = userId;
        this.name = name;
        this.totalBalance = totalBalance;
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return userId;
    }

    public void setUuid(UUID uuid) {
        this.userId = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(Float totalBalance) {
        this.totalBalance = totalBalance;
    }

}



