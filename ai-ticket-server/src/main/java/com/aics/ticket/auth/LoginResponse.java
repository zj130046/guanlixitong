package com.aics.ticket.auth;

import com.aics.ticket.common.enums.IdentityType;

public class LoginResponse {

    private String token;
    private String username;
    private IdentityType identityType;
    private Long id;
    private String displayName;

    public LoginResponse() {
    }

    public LoginResponse(String token, String username, IdentityType identityType) {
        this(token, username, identityType, null, username);
    }

    public LoginResponse(String token, String username, IdentityType identityType, Long id, String displayName) {
        this.token = token;
        this.username = username;
        this.identityType = identityType;
        this.id = id;
        this.displayName = displayName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public IdentityType getIdentityType() {
        return identityType;
    }

    public void setIdentityType(IdentityType identityType) {
        this.identityType = identityType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
