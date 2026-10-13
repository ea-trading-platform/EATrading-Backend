package com.eatrading.api.entities;

import java.util.UUID;
import com.eatrading.api.objects.Asset;

/**
 * WatchlistItem
 * Represents a stock on a client's watchlist
 * Maps to the Asset stored in the client_watchlist collection table
 */
public class ClientWatchList {

    private UUID clientId;
    private Asset asset;

    public ClientWatchList() {
        // Default constructor
    }

    public ClientWatchList(UUID clientId, Asset asset) {
        this.clientId = clientId;
        this.asset = asset;
    }

    // Getters
    public UUID getClientId() {
        return this.clientId;
    }

    public Asset getAsset() {
        return this.asset;
    }

    public String getSymbol() {
        return this.asset != null ? this.asset.getSymbol() : null;
    }

    // Setters
    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }
}
