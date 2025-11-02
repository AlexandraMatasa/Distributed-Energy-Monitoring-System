package com.example.authmanagement.dtos;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO used for communication with User Management microservice
 */
public class UserServiceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String username;
    private String password;
    private String role;
    private String email;
    private String fullName;

    public UserServiceDTO() {
    }

    public UserServiceDTO(UUID id, String username, String password, String role, String email, String fullName) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.fullName = fullName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}