package com.project.flightOps.responsedto;

import com.project.flightOps.enums.NotificationCategory;
import com.project.flightOps.enums.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String notificationId;
    private String userId;
    private String userName;
    private String message;
    private NotificationCategory category;
    private NotificationStatus status;
    private LocalDateTime createdDate;
}
