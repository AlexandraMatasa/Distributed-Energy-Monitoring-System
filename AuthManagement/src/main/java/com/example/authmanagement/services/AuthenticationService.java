package com.example.authmanagement.services;

import com.example.authmanagement.config.JwtTokenUtil;
import com.example.authmanagement.dtos.*;
import com.example.authmanagement.entities.Credential;
import com.example.authmanagement.repositories.CredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8080}")
    private String userServiceUrl;

    @Transactional
    public void register(RegisterRequest request) throws Exception {
        LOGGER.info("Starting registration for username: {}", request.getUsername());

        if (credentialRepository.existsByUsername(request.getUsername())) {
            LOGGER.error("Username {} already exists", request.getUsername());
            throw new Exception("Username already exists");
        }

        UUID userId = createUserInUserService(request);
        LOGGER.info("User created in User Management service with ID: {}", userId);

        try {
            Credential credential = new Credential();
            credential.setUserId(userId);
            credential.setUsername(request.getUsername());
            credential.setPassword(passwordEncoder.encode(request.getPassword()));
            credential.setRole(request.getRole());

            credentialRepository.save(credential);
            LOGGER.info("Credentials saved for user: {}", request.getUsername());

            LOGGER.info("Registration completed successfully for user: {}", request.getUsername());

        } catch (Exception e) {
            LOGGER.error("Failed to save credentials, rolling back user creation", e);
            deleteUserFromUserService(userId);
            throw new Exception("Registration failed: " + e.getMessage());
        }
    }

    public JwtResponse login(JwtRequest request) throws Exception {
        LOGGER.info("Login attempt for username: {}", request.getUsername());

        authenticate(request.getUsername(), request.getPassword());

        Credential credential = credentialRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new Exception("User not found"));

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

    @Transactional
    public void deleteCredentialsByUserId(UUID userId) {
        LOGGER.info("Deleting credentials for userId: {}", userId);
        credentialRepository.deleteByUserId(userId);
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

    private UUID createUserInUserService(RegisterRequest request) throws Exception {
        try {
            String url = userServiceUrl + "/user";

            UserServiceDTO userDTO = new UserServiceDTO();
            userDTO.setUsername(request.getUsername());
            userDTO.setPassword(request.getPassword());
            userDTO.setRole(request.getRole());
            userDTO.setEmail(request.getEmail());
            userDTO.setFullName(request.getFullName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserServiceDTO> entity = new HttpEntity<>(userDTO, headers);

            ResponseEntity<Void> response = restTemplate.postForEntity(url, entity, Void.class);

            String location = response.getHeaders().getLocation().toString();
            String userIdStr = location.substring(location.lastIndexOf('/') + 1);

            return UUID.fromString(userIdStr);

        } catch (Exception e) {
            LOGGER.error("Failed to create user in User Management service: {}", e.getMessage());
            throw new Exception("Failed to create user in User Management service: " + e.getMessage());
        }
    }

    private void deleteUserFromUserService(UUID userId) {
        try {
            String url = userServiceUrl + "/user/" + userId;
            restTemplate.delete(url);
            LOGGER.info("User deleted from User Management service: {}", userId);
        } catch (Exception e) {
            LOGGER.error("Failed to delete user from User Management service: {}", e.getMessage());
        }
    }
}