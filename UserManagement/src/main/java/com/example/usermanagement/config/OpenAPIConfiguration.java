package com.example.usermanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
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
        description = "JWT token from Auth Service"
)
@OpenAPIDefinition(
        info = @Info(
                title = "User Management API",
                version = "1.0",
                description = "Service for managing user accounts and profiles"
        ),
        security = { @SecurityRequirement(name = "bearerAuth") }
)
public class OpenAPIConfiguration {

    @Bean
    public OpenApiCustomizer customizeOpenApi() {
        return openApi -> {
            Map<String, PathItem> paths = openApi.getPaths();

            customizeOperation(paths, "/user", PathItem::getGet,
                    "List all users",
                    "Retrieves list of all users in the system. Requires ADMIN role.",
                    Map.of(
                            "200", new ApiResponse().description("List of users retrieved successfully"),
                            "403", new ApiResponse().description("Forbidden - Only ADMIN can list users")
                    ));

            customizeOperation(paths, "/user", PathItem::getPost,
                    "Create new user",
                    "Creates a new user account. This endpoint is called internally by Auth Service during registration.",
                    Map.of(
                            "201", new ApiResponse()
                                    .description("User created successfully")
                                    .headers(Map.of("Location",
                                            new io.swagger.v3.oas.models.headers.Header()
                                                    .description("URI of the created user")
                                                    .schema(new Schema<>().type("string")))),
                            "400", badRequest(),
                            "403", new ApiResponse().description("Forbidden - Insufficient permissions")
                    ));

            customizeOperation(paths, "/user/{id}", PathItem::getGet,
                    "Get user by ID",
                    "Retrieves detailed information about a specific user. Requires ADMIN role.",
                    Map.of(
                            "200", new ApiResponse().description("User details retrieved successfully"),
                            "403", new ApiResponse().description("Forbidden - Only ADMIN can view user details"),
                            "404", new ApiResponse().description("User not found")
                    ));

            customizeOperation(paths, "/user/{id}", PathItem::getPut,
                    "Update user",
                    "Updates user information. Requires ADMIN role.",
                    Map.of(
                            "204", new ApiResponse().description("User updated successfully"),
                            "400", badRequest(),
                            "403", new ApiResponse().description("Forbidden - Only ADMIN can update users"),
                            "404", new ApiResponse().description("User not found")
                    ));

            customizeOperation(paths, "/user/{id}", PathItem::getDelete,
                    "Delete user",
                    "Deletes a user from the system. Triggers cascade deletion in Auth and Device services. Requires ADMIN role.",
                    Map.of(
                            "204", new ApiResponse().description("User deleted successfully"),
                            "403", new ApiResponse().description("Forbidden - Only ADMIN can delete users"),
                            "404", new ApiResponse().description("User not found")
                    ));

            paths.values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        if (!operation.getResponses().containsKey("500")) {
                            operation.getResponses().addApiResponse("500",
                                    new ApiResponse().description("Internal server error"));
                        }
                    }));
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

    private ApiResponse badRequest() {
        return new ApiResponse()
                .description("Bad request - Invalid input data")
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(errorSchema())));
    }

    private Schema<?> errorSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("message", new Schema<>()
                        .type("string")
                        .description("Error message"));
    }
}