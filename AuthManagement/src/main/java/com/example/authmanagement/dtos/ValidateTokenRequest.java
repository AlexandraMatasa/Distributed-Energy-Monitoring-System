package com.example.authmanagement.dtos;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public class ValidateTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Token is required")
    private String token;

    public ValidateTokenRequest() {
    }

    public ValidateTokenRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}