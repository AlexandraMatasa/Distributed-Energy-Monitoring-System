package com.example.usermanagement.services;

import com.example.usermanagement.config.RabbitMQConfig;
import com.example.usermanagement.dtos.SyncMessageDTO;
import com.example.usermanagement.dtos.UserDTO;
import com.example.usermanagement.dtos.UserDetailsDTO;
import com.example.usermanagement.dtos.builders.UserBuilder;
import com.example.usermanagement.entities.User;
import com.example.usermanagement.handlers.exceptions.model.DuplicateResourceException;
import com.example.usermanagement.handlers.exceptions.model.ResourceNotFoundException;
import com.example.usermanagement.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public UserService(UserRepository userRepository, RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.USER_SYNC_QUEUE)
    @Transactional
    public void handleSyncMessage(SyncMessageDTO message) {
        LOGGER.info("User received sync message: {}", message.getEventType());

        try {
            switch (message.getEventType()) {
                case "USER_CREATED":
                    handleUserCreated(message);
                    break;
                case "USER_DELETED":
                    LOGGER.debug("Ignoring USER_DELETED echo");
                    break;
                case "DEVICE_CREATED":
                case "DEVICE_DELETED":
                    LOGGER.debug("Ignoring device event: {}", message.getEventType());
                    break;
                default:
                    LOGGER.debug("Ignoring event: {}", message.getEventType());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process sync message: {}", e.getMessage(), e);
        }
    }

    private void handleUserCreated(SyncMessageDTO message) {
        LOGGER.info("Received USER_CREATED: credentialsId={}, username={}",
                message.getCredentialsId(), message.getUsername());

        try {
            if (userRepository.existsByUsername(message.getUsername())) {
                LOGGER.error("Username {} already exists", message.getUsername());
                publishUserCreateFailed(message.getCredentialsId(),
                        "Username already exists: " + message.getUsername());
                return;
            }

            User user = new User();
            user.setUsername(message.getUsername());
            user.setPassword(message.getPassword());
            user.setEmail(message.getEmail());
            user.setFullName(message.getFullName());
            user.setRole(message.getRole());

            user = userRepository.save(user);
            LOGGER.info("User created: userId={}, username={}", user.getId(), user.getUsername());

            publishUserIdAssigned(message.getCredentialsId(), user.getId(), user.getUsername());

        } catch (Exception e) {
            LOGGER.error("Failed to create user: {}", e.getMessage(), e);
            publishUserCreateFailed(message.getCredentialsId(), e.getMessage());
        }
    }

    private void publishUserIdAssigned(UUID credentialsId, UUID userId, String username) {
        try {
            SyncMessageDTO response = new SyncMessageDTO();
            response.setEventType("USER_ID_ASSIGNED");
            response.setCredentialsId(credentialsId);
            response.setUserId(userId);
            response.setUsername(username);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    "",
                    response
            );

            LOGGER.info("Published USER_ID_ASSIGNED: credentialsId={}, userId={}",
                    credentialsId, userId);
        } catch (Exception e) {
            LOGGER.error("Failed to publish USER_ID_ASSIGNED: {}", e.getMessage());
            userRepository.deleteById(userId);
            publishUserCreateFailed(credentialsId, "Failed to publish USER_ID_ASSIGNED");
        }
    }

    private void publishUserCreateFailed(UUID credentialsId, String errorMessage) {
        try {
            SyncMessageDTO response = new SyncMessageDTO();
            response.setEventType("USER_CREATE_FAILED");
            response.setCredentialsId(credentialsId);
            response.setUserId(null);
            response.setErrorMessage(errorMessage);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    "",
                    response
            );

            LOGGER.error("Published USER_CREATE_FAILED: credentialsId={}, error={}",
                    credentialsId, errorMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to publish USER_CREATE_FAILED: {}", e.getMessage());
        }
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

        publishUserDeleted(id);
    }

    private void publishUserDeleted(UUID userId) {
        try {
            SyncMessageDTO message = new SyncMessageDTO();
            message.setEventType("USER_DELETED");
            message.setUserId(userId);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    "",
                    message
            );

            LOGGER.info("Published USER_DELETED for userId: {}", userId);
        } catch (Exception e) {
            LOGGER.error("Failed to publish USER_DELETED: {}", e.getMessage());
        }
    }
}