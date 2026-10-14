package com.eatrading.api.dto;

import java.util.UUID;

import com.eatrading.api.objects.Role;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UUID clientId;
    private long expiresIn; // in seconds
    private String tokenType;
    private Role role;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String refreshToken, UUID clientId, long expiresIn) {
        this(accessToken, refreshToken, clientId, expiresIn, Role.CLIENT);
    }

    public AuthResponse(String accessToken, String refreshToken, UUID clientId, long expiresIn, Role role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.clientId = clientId;
        this.expiresIn = expiresIn;
        this.tokenType = "Bearer";
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
