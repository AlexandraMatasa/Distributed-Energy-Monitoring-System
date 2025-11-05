package com.example.devicemanagement.config;

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
                title = "Device Management API",
                version = "1.0",
                description = "Service for managing IoT devices and user-device assignments"
        ),
        security = { @SecurityRequirement(name = "bearerAuth") }
)
public class OpenAPIConfiguration {

    @Bean
    public OpenApiCustomizer customizeOpenApi() {
        return openApi -> {
            Map<String, PathItem> paths = openApi.getPaths();

            customizeOperation(paths, "/device", PathItem::getGet,
                    "List all devices",
                    "Retrieves list of all devices. Requires ADMIN role.",
                    Map.of(
                            "200", new ApiResponse().description("List of devices retrieved successfully"),
                            "403", forbidden()
                    ));

            customizeOperation(paths, "/device", PathItem::getPost,
                    "Create new device",
                    "Creates a new IoT device. Requires ADMIN role.",
                    Map.of(
                            "201", new ApiResponse().description("Device created successfully"),
                            "400", badRequest(),
                            "403", forbidden()
                    ));

            customizeOperation(paths, "/device/{id}", PathItem::getGet,
                    "Get device by ID",
                    "Retrieves detailed information about a specific device. Requires ADMIN role.",
                    Map.of(
                            "200", new ApiResponse().description("Device details retrieved successfully"),
                            "403", forbidden(),
                            "404", notFound("Device not found")
                    ));

            customizeOperation(paths, "/device/{id}", PathItem::getPut,
                    "Update device",
                    "Updates device information. Requires ADMIN role.",
                    Map.of(
                            "204", new ApiResponse().description("Device updated successfully"),
                            "400", badRequest(),
                            "403", forbidden(),
                            "404", notFound("Device not found")
                    ));

            customizeOperation(paths, "/device/{id}", PathItem::getDelete,
                    "Delete device",
                    "Deletes a device from the system. Requires ADMIN role.",
                    Map.of(
                            "204", new ApiResponse().description("Device deleted successfully"),
                            "403", forbidden(),
                            "404", notFound("Device not found")
                    ));

            customizeOperation(paths, "/device/user", PathItem::getPost,
                    "Assign device to user",
                    "Assigns a device to a specific user. Requires ADMIN role.",
                    Map.of(
                            "204", new ApiResponse().description("Device assigned successfully"),
                            "400", badRequest(),
                            "403", forbidden(),
                            "404", notFound("Device or user not found")
                    ));

            customizeOperation(paths, "/device/user/{deviceId}", PathItem::getDelete,
                    "Unassign device from user",
                    "Removes device assignment from user. Requires ADMIN role.",
                    Map.of(
                            "204", new ApiResponse().description("Device unassigned successfully"),
                            "403", forbidden(),
                            "404", notFound("Device not found")
                    ));

            customizeOperation(paths, "/device/user/{userId}", PathItem::getGet,
                    "Get devices assigned to user",
                    "Retrieves all devices assigned to a specific user. ADMIN can view any user's devices, CLIENT can only view their own.",
                    Map.of(
                            "200", new ApiResponse().description("User's devices retrieved successfully"),
                            "403", forbidden(),
                            "404", notFound("User not found")
                    ));

            customizeOperation(paths, "/device/sync/user-created", PathItem::getPost,
                    "Sync user creation",
                    "Internal endpoint to synchronize user creation from User Service",
                    Map.of(
                            "200", new ApiResponse().description("User cached successfully")
                    ));

            customizeOperation(paths, "/device/sync/user-deleted", PathItem::getPost,
                    "Sync user deletion",
                    "Internal endpoint to synchronize user deletion from User Service",
                    Map.of(
                            "200", new ApiResponse().description("User removed and assignments deleted")
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

    private ApiResponse forbidden() {
        return new ApiResponse().description("Forbidden - Only ADMIN can perform this operation");
    }

    private ApiResponse badRequest() {
        return new ApiResponse()
                .description("Bad request - Invalid input data")
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(errorSchema())));
    }

    private ApiResponse notFound(String message) {
        return new ApiResponse().description(message);
    }

    private Schema<?> errorSchema() {
        return new Schema<>()
                .type("object")
                .addProperty("message", new Schema<>()
                        .type("string")
                        .description("Error message"));
    }
}