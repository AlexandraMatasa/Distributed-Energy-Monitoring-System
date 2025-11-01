package com.example.usermanagement.dtos.builders;

import com.example.usermanagement.dtos.UserDTO;
import com.example.usermanagement.dtos.UserDetailsDTO;
import com.example.usermanagement.entities.User;

public class UserBuilder {

    private UserBuilder() {
    }

    public static UserDTO toUserDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getFullName()
        );
    }

    public static UserDetailsDTO toUserDetailsDTO(User user) {
        return new UserDetailsDTO(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getEmail(),
                user.getFullName()
        );
    }

    public static User toEntity(UserDetailsDTO userDetailsDTO) {
        return new User(
                userDetailsDTO.getUsername(),
                userDetailsDTO.getPassword(),
                userDetailsDTO.getRole(),
                userDetailsDTO.getEmail(),
                userDetailsDTO.getFullName()
        );
    }
}
