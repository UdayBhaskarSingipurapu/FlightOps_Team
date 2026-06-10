package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuditLogRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Action is required")
    private String action;

    @NotBlank(message = "Entity type is required")
    private String entityType;
}
