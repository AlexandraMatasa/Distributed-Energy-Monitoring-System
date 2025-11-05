package com.example.authmanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT token obtained from /auth/login"
)
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Management API",
                version = "1.0",
                description = "Authentication and authorization service for the distributed system"
        ),
        security = { @SecurityRequirement(name = "bearerAuth") }
)
public class OpenAPIConfiguration {

    @Bean
    public OpenApiCustomizer customizeOpenApi() {
        return openApi -> {
            Map<String, PathItem> paths = openApi.getPaths();

            customizeOperation(paths, "/auth/register", PathItem::getPost,
                    "Register new user",
                    "Creates a new user account. Only accessible by ADMIN users.",
                    Map.of(
                            "201", new ApiResponse().description("User registered successfully"),
                            "400", errorResponse("Invalid input data or username already exists"),
                            "403", errorResponse("Only ADMIN can register new users")
                    ));

            customizeOperation(paths, "/auth/login", PathItem::getPost,
                    "User login",
                    "Authenticates user and returns JWT token",
                    Map.of(
                            "200", new ApiResponse().description("Login successful, returns JWT token and user details")
                                    .content(new Content().addMediaType("application/json",
                                            new MediaType().schema(createJwtResponseSchema()))),
                            "401", errorResponse("Invalid credentials")
                    ));

            customizeOperation(paths, "/auth/validate", PathItem::getGet,
                    "Validate JWT token",
                    "Validates JWT token from Authorization header",
                    Map.of(
                            "200", new ApiResponse().description("Token is valid, returns user ID and role")
                                    .content(new Content().addMediaType("application/json",
                                            new MediaType().schema(createValidateResponseSchema()))),
                            "401", new ApiResponse().description("Token is invalid or missing")
                    ));

            customizeOperation(paths, "/auth/validate", PathItem::getPost,
                    "Validate JWT token (alternative)",
                    "Alternative method to validate token from request body",
                    Map.of(
                            "200", new ApiResponse().description("Token is valid"),
                            "401", new ApiResponse().description("Token is invalid or missing")
                    ));

            customizeOperation(paths, "/auth/sync/user-deleted", PathItem::getPost,
                    "Sync user deletion",
                    "Internal endpoint to synchronize user deletion from User Service",
                    Map.of(
                            "204", new ApiResponse().description("Credentials deleted successfully"),
                            "500", new ApiResponse().description("Internal server error")
                    ));

            paths.values().forEach(pathItem -> {
                pathItem.readOperations().forEach(operation -> {
                    ApiResponses responses = operation.getResponses();

                    if (!responses.containsKey("500") && !operation.getOperationId().contains("sync")) {
                        responses.addApiResponse("500",
                                new ApiResponse().description("Internal server error"));
                    }
                });
            });
        };
    }

    private void customizeOperation(
            Map<String, PathItem> paths,
            String path,
            java.util.function.Function<PathItem, Operation> operationGetter,
            String summary,
            String description,
            Map<String, ApiResponse> responses) {

        PathItem pathItem = paths.get(path);
        if (pathItem == null) return;

        Operation operation = operationGetter.apply(pathItem);
        if (operation == null) return;

        operation.setSummary(summary);
        operation.setDescription(description);

        responses.forEach((code, response) ->
                operation.getResponses().addApiResponse(code, response));
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>()
                                .type("object")
                                .addProperty("message", new Schema<>()
                                        .type("string")
                                        .description("Error message")))));
    }

    private Schema<?> createJwtResponseSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("token", new Schema<>()
                        .type("string")
                        .description("JWT authentication token"))
                .addProperty("userId", new Schema<>()
                        .type("string")
                        .format("uuid")
                        .description("User unique identifier"))
                .addProperty("role", new Schema<>()
                        .type("string")
                        .description("User role (ADMIN or CLIENT)")
                        .example("ADMIN"));
    }

    private Schema<?> createValidateResponseSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("valid", new Schema<>()
                        .type("boolean")
                        .description("Whether the token is valid"))
                .addProperty("userId", new Schema<>()
                        .type("string")
                        .format("uuid")
                        .description("User ID from token"))
                .addProperty("role", new Schema<>()
                        .type("string")
                        .description("User role from token"));
    }
}