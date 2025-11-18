package com.example.authmanagement.services;

import com.example.authmanagement.config.JwtTokenUtil;
import com.example.authmanagement.config.RabbitMQConfig;
import com.example.authmanagement.dtos.*;
import com.example.authmanagement.entities.Credential;
import com.example.authmanagement.repositories.CredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public AuthenticationService(CredentialRepository credentialRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenUtil jwtTokenUtil,
                                 AuthenticationManager authenticationManager,
                                 RabbitTemplate rabbitTemplate) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.authenticationManager = authenticationManager;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void register(RegisterRequest request) throws Exception {
        LOGGER.info("Starting registration for username: {}", request.getUsername());

        if (credentialRepository.existsByUsername(request.getUsername())) {
            LOGGER.error("Username {} already exists", request.getUsername());
            throw new Exception("Username already exists");
        }

        Credential credential = new Credential();
        credential.setUsername(request.getUsername());
        credential.setPassword(passwordEncoder.encode(request.getPassword()));
        credential.setRole(request.getRole());
        credential.setUserId(null);

        credential = credentialRepository.save(credential);
        LOGGER.info("Credentials created with id: {}", credential.getId());

        publishUserCreated(credential.getId(), request);
    }

    private void publishUserCreated(UUID credentialsId, RegisterRequest request) {
        try {
            SyncMessageDTO message = new SyncMessageDTO();
            message.setEventType("USER_CREATED");
            message.setCredentialsId(credentialsId);
            message.setUsername(request.getUsername());
            message.setPassword(request.getPassword());
            message.setEmail(request.getEmail());
            message.setFullName(request.getFullName());
            message.setRole(request.getRole());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    "",
                    message
            );

            LOGGER.info("Published USER_CREATED for credentialsId: {}", credentialsId);
        } catch (Exception e) {
            LOGGER.error("Failed to publish USER_CREATED: {}", e.getMessage());
            credentialRepository.deleteById(credentialsId);
            throw new RuntimeException("Failed to publish user creation event", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.AUTH_SYNC_QUEUE)
    @Transactional
    public void handleSyncMessage(SyncMessageDTO message) {
        LOGGER.info("Auth received sync message: {}", message.getEventType());

        try {
            switch (message.getEventType()) {
                case "USER_ID_ASSIGNED":
                    handleUserIdAssigned(message);
                    break;
                case "USER_CREATE_FAILED":
                    handleUserCreateFailed(message);
                    break;
                case "USER_DELETED":
                    handleUserDeleted(message);
                    break;
                case "USER_CREATED":
                    LOGGER.debug("Ignoring USER_CREATED echo");
                    break;
                default:
                    LOGGER.debug("Ignoring event: {}", message.getEventType());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process sync message: {}", e.getMessage(), e);
        }
    }

    private void handleUserIdAssigned(SyncMessageDTO message) {
        LOGGER.info("Received USER_ID_ASSIGNED: credentialsId={}, userId={}",
                message.getCredentialsId(), message.getUserId());

        try {
            Optional<Credential> credentialOptional = credentialRepository
                    .findById(message.getCredentialsId());

            if (credentialOptional.isPresent()) {
                Credential credential = credentialOptional.get();
                credential.setUserId(message.getUserId());
                credentialRepository.save(credential);

                LOGGER.info("Updated credentials {} with userId: {}",
                        message.getCredentialsId(), message.getUserId());
            } else {
                LOGGER.error("Credentials {} not found", message.getCredentialsId());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to update credentials: {}", e.getMessage());
        }
    }

    private void handleUserCreateFailed(SyncMessageDTO message) {
        LOGGER.error("Received USER_CREATE_FAILED: credentialsId={}, error={}",
                message.getCredentialsId(), message.getErrorMessage());

        try {
            if (message.getCredentialsId() != null) {
                credentialRepository.deleteById(message.getCredentialsId());
                LOGGER.info("Rolled back credentials: {}", message.getCredentialsId());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to rollback credentials: {}", e.getMessage());
        }
    }

    private void handleUserDeleted(SyncMessageDTO message) {
        LOGGER.info("Received USER_DELETED: userId={}", message.getUserId());

        try {
            Optional<Credential> credentialOptional = credentialRepository
                    .findByUserId(message.getUserId());

            if (credentialOptional.isPresent()) {
                credentialRepository.delete(credentialOptional.get());
                LOGGER.info("Deleted credentials for userId: {}", message.getUserId());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete credentials: {}", e.getMessage());
        }
    }

    public JwtResponse login(JwtRequest request) throws Exception {
        LOGGER.info("Login attempt for username: {}", request.getUsername());

        authenticate(request.getUsername(), request.getPassword());

        Credential credential = credentialRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new Exception("User not found"));

        if (credential.getUserId() == null) {
            LOGGER.error("User {} not fully registered yet", request.getUsername());
            throw new Exception("User registration not completed. Please try again later.");
        }

        String token = jwtTokenUtil.generateToken(credential.getUserId(), credential.getRole());
        LOGGER.info("Login successful for user: {}", request.getUsername());

        return new JwtResponse(token, credential.getUserId(), credential.getRole());
    }

    public ValidateTokenResponse validateToken(String token) {
        try {
            if (jwtTokenUtil.isTokenValid(token)) {
                UUID userId = jwtTokenUtil.extractUserId(token);
                String role = jwtTokenUtil.extractRole(token);

                LOGGER.info("Token validated successfully for userId: {}", userId);
                return new ValidateTokenResponse(true, userId, role);
            } else {
                LOGGER.warn("Invalid token provided");
                return new ValidateTokenResponse(false, null, null);
            }
        } catch (Exception e) {
            LOGGER.error("Token validation failed: {}", e.getMessage());
            return new ValidateTokenResponse(false, null, null);
        }
    }

    public ValidateTokenResponse validateTokenForForwardAuth(String authHeader) {
        String token = extractToken(authHeader);

        if (token == null) {
            LOGGER.warn("No token provided in request");
            return null;
        }

        ValidateTokenResponse validation = validateToken(token);

        if (!validation.isValid()) {
            LOGGER.warn("Invalid token provided");
            return null;
        }

        LOGGER.info("Token validated successfully for userId: {}, role: {}",
                validation.getUserId(), validation.getRole());

        return validation;
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            LOGGER.debug("Token extracted from Authorization header (Traefik ForwardAuth)");
            return authHeader.substring(7);
        }
        return null;
    }

    private void authenticate(String username, String password) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (DisabledException e) {
            LOGGER.error("User disabled: {}", username);
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            LOGGER.error("Invalid credentials for user: {}", username);
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}