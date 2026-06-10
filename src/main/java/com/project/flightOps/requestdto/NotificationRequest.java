package com.project.flightOps.requestdto;

import com.project.flightOps.enums.NotificationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Category is required")
    private NotificationCategory category;
}
