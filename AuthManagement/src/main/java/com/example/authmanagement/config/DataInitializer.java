package com.example.authmanagement.config;

import com.example.authmanagement.dtos.RegisterRequest;
import com.example.authmanagement.repositories.CredentialRepository;
import com.example.authmanagement.services.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(
            CredentialRepository credentialRepository,
            AuthenticationService authenticationService,
            RabbitTemplate rabbitTemplate) {
        return args -> {
            waitForRabbitMQ(rabbitTemplate);

            if (!credentialRepository.existsByUsername("admin")) {
                LOGGER.info("Creating default admin user...");
                RegisterRequest adminRequest = new RegisterRequest();
                adminRequest.setUsername("admin");
                adminRequest.setPassword("admin123");
                adminRequest.setEmail("admin@example.com");
                adminRequest.setFullName("System Administrator");
                adminRequest.setRole("ADMIN");

                try {
                    authenticationService.register(adminRequest);
                    LOGGER.info("Default admin user created successfully!");
                } catch (Exception e) {
                    LOGGER.error("Failed to create default admin: {}", e.getMessage());
                }
            }
        };
    }

    private void waitForRabbitMQ(RabbitTemplate rabbitTemplate) {
        int maxRetries = 30;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                // Verifică doar conexiunea, nu căuta un queue specific
                rabbitTemplate.execute(channel -> {
                    // Verifică că poate declara un exchange temporar
                    channel.exchangeDeclarePassive(RabbitMQConfig.SYNC_EXCHANGE);
                    return null;
                });
                LOGGER.info("RabbitMQ is ready!");
                return;
            } catch (Exception e) {
                retryCount++;
                LOGGER.info("Waiting for RabbitMQ... (attempt {}/{})", retryCount, maxRetries);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        LOGGER.error("RabbitMQ not available after {} attempts.", maxRetries);
        throw new RuntimeException("RabbitMQ not available - cannot start application");
    }
}