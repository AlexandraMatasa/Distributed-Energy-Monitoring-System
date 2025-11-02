package com.example.usermanagement.services;

import com.example.usermanagement.dtos.UserDTO;
import com.example.usermanagement.dtos.UserDetailsDTO;
import com.example.usermanagement.dtos.builders.UserBuilder;
import com.example.usermanagement.entities.User;
import com.example.usermanagement.handlers.exceptions.model.DuplicateResourceException;
import com.example.usermanagement.handlers.exceptions.model.ResourceNotFoundException;
import com.example.usermanagement.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${device.service.url:http://localhost:8081}")
    private String deviceServiceUrl;

    @Value("${auth.service.url:http://localhost:8083}")
    private String authServiceUrl;

    @Autowired
    public UserService(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    public List<UserDTO> findUsers() {
        List<User> userList = userRepository.findAll();
        return userList.stream()
                .map(UserBuilder::toUserDTO)
                .collect(Collectors.toList());
    }

    public UserDetailsDTO findUserById(UUID id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (!userOptional.isPresent()) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }
        return UserBuilder.toUserDetailsDTO(userOptional.get());
    }

    public UUID insert(UserDetailsDTO userDTO) {
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            LOGGER.error("User with username {} already exists", userDTO.getUsername());
            throw new DuplicateResourceException("User with username: " + userDTO.getUsername());
        }

        User user = UserBuilder.toEntity(userDTO);
        user = userRepository.save(user);
        UUID userId = user.getId();
        LOGGER.debug("User with id {} was inserted in db", user.getId());

        notifyDeviceServiceUserCreated(userId);

        return user.getId();
    }

    public UUID update(UUID id, UserDetailsDTO userDTO) {
        Optional<User> userOptional = userRepository.findById(id);
        if (!userOptional.isPresent()) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }

        User user = userOptional.get();

        if (!user.getUsername().equals(userDTO.getUsername()) &&
                userRepository.existsByUsername(userDTO.getUsername())) {
            LOGGER.error("User with username {} already exists", userDTO.getUsername());
            throw new DuplicateResourceException("User with username: " + userDTO.getUsername());
        }

        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        user.setRole(userDTO.getRole());
        user.setEmail(userDTO.getEmail());
        user.setFullName(userDTO.getFullName());

        userRepository.save(user);
        LOGGER.debug("User with id {} was updated in db", id);
        return id;
    }

    public void delete(UUID id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (!userOptional.isPresent()) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }
        userRepository.deleteById(id);
        LOGGER.debug("User with id {} was deleted from db", id);

        notifyAuthServiceUserDeleted(id);
        notifyDeviceServiceUserDeleted(id);
    }

    private void notifyDeviceServiceUserCreated(UUID userId) {
        try {
            String url = deviceServiceUrl + "/device/sync/user-created";
            LOGGER.info("Notifying Device Service about user {} creation", userId);

            restTemplate.postForEntity(url, userId, Void.class);

            LOGGER.info("Device Service notified successfully");
        } catch (Exception e) {
            LOGGER.warn("Failed to notify Device Service: {}", e.getMessage());
        }
    }

    private void notifyDeviceServiceUserDeleted(UUID userId) {
        try {
            String url = deviceServiceUrl + "/device/sync/user-deleted";
            LOGGER.info("Notifying Device Service about user {} deletion", userId);

            restTemplate.postForEntity(url, userId, Void.class);

            LOGGER.info("Device Service notified successfully");
        } catch (Exception e) {
            LOGGER.warn("Failed to notify Device Service: {}", e.getMessage());
        }
    }

    private void notifyAuthServiceUserDeleted(UUID userId) {
        try {
            String url = authServiceUrl + "/auth/sync/user-deleted";
            LOGGER.info("Notifying Auth Service about user {} deletion", userId);

            restTemplate.postForEntity(url, userId, Void.class);

            LOGGER.info("Auth Service notified successfully");
        } catch (Exception e) {
            LOGGER.warn("Failed to notify Auth Service: {}", e.getMessage());
        }
    }
}