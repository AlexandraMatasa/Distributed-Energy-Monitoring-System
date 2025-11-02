package com.example.authmanagement.dtos;

import java.io.Serializable;
import java.util.UUID;

public class JwtResponse implements Serializable {

    private static final long serialVersionUID = -8091879091924046844L;

    private final String token;
    private final UUID userId;
    private final String role;

    public JwtResponse(String token, UUID userId, String role) {
        this.token = token;
        this.userId = userId;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}