package com.example.usermanagement.dtos;

import java.util.Objects;
import java.util.UUID;

public class UserDTO {
    private UUID id;
    private String username;
    private String role;
    private String fullName;
    private String email;

    public UserDTO() {}

    public UserDTO(UUID id, String username, String role, String fullName, String email) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDTO userDTO = (UserDTO) o;
        return Objects.equals(username, userDTO.username) &&
                Objects.equals(role, userDTO.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, role);
    }
}