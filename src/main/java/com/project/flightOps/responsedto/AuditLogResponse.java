package com.project.flightOps.responsedto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private String auditId;
    private String userEmail;
    private String userName;
    private String userRole;
    private String action;
    private String entityType;
    private LocalDateTime timestamp;
}
