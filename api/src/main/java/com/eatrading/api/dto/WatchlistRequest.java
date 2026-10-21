package com.eatrading.api.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.eatrading.api.objects.Instrument;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class WatchlistRequest {

    @JsonAlias("client_id")
    @NotNull(message = "clientId/client_id is required")
    private UUID clientId;

    @NotBlank(message = "symbol is required")
    private String symbol;

    @JsonAlias("instrumentType")
    private Instrument instrument = Instrument.EQUITY;

    // Constructors
    public WatchlistRequest() {
    }

    public WatchlistRequest(UUID clientId, String symbol) {
        this.clientId = clientId;
        this.symbol = symbol;
    }

    public WatchlistRequest(UUID clientId, String symbol, Instrument instrument) {
        this.clientId = clientId;
        this.symbol = symbol;
        this.instrument = instrument;
    }

    // Getters and Setters
    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }
}
