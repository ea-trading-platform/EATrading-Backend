package com.eatrading.api.entities;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "client")
public class Client extends User {

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "client_watchlist", joinColumns = @JoinColumn(name = "client_id"))
    private Set<Asset> watchlist;

    public Client() {
        super();
        // Default constructor for JPA
    }
    
    public Client(String name, String email) {
        super(name, email);
        this.portfolio = new Portfolio(); 
        this.watchlist = new HashSet<>();
    }

    public void addHolding(Holding holding) {
        this.portfolio.addHolding(holding);
    }
    public void removeHolding(Holding holding) {
        this.portfolio.removeHolding(holding);
    }

    public Holding getHolding(String holdingKey) {
        Holding result = portfolio.findHoldingFromPortfolio(holdingKey);
        if (result.getAsset() != null) {
            return result;
        }

        return null;
    }

    public void addToWatchlist(Asset asset) {
        watchlist.add(asset);
    }

    public Iterable<Asset> getWatchlist() {
        return this.watchlist;
    }

    public Holding getUSDHolding() {
        return this.getHolding("USD");
    }

    public BigDecimal getPortfolioValue() {
        return portfolio.getPortfolioValue();
    }
}
