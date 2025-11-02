package com.example.authmanagement.controllers;

import com.example.authmanagement.dtos.*;
import com.example.authmanagement.services.AuthenticationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
@Validated
public class AuthenticationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        try {
            LOGGER.info("Registration request received for username: {}", request.getUsername());
            authenticationService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully. Please login.");
        } catch (Exception e) {
            LOGGER.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody JwtRequest request) {
        try {
            LOGGER.info("Login request received for username: {}", request.getUsername());
            JwtResponse response = authenticationService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateTokenResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        LOGGER.info("Token validation request received");
        ValidateTokenResponse response = authenticationService.validateToken(request.getToken());

        if (response.isValid()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/sync/user-deleted")
    public ResponseEntity<Void> syncUserDeleted(@RequestBody java.util.UUID userId) {
        try {
            LOGGER.info("Sync request received for user deletion: {}", userId);
            authenticationService.deleteCredentialsByUserId(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.error("Failed to sync user deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}