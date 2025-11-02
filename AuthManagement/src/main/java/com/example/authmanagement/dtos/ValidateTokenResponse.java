package com.example.authmanagement.dtos;

import java.io.Serializable;
import java.util.UUID;

public class ValidateTokenResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean valid;
    private UUID userId;
    private String role;

    public ValidateTokenResponse() {
    }

    public ValidateTokenResponse(boolean valid, UUID userId, String role) {
        this.valid = valid;
        this.userId = userId;
        this.role = role;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}