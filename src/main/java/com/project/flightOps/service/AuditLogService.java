package com.project.flightOps.service;

import com.project.flightOps.entity.AuditLog;
import com.project.flightOps.entity.User;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.AuditLogRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.AuditLogRequest;
import com.project.flightOps.responsedto.AuditLogResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 1. Added SLF4J Import
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // 2. Added Lombok Logger Annotation
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    // Called internally (e.g., after login, after turnaround complete, after status change)
    @Transactional
    public void log(String userId, String action, String entityType) {
        log.debug("Internal log request received for userId: {}, action: {}, entityType: {}", userId, action, entityType);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            // 3. Warning instead of throwing error, keeping the "silent fail" philosophy but leaving a paper trail
            log.warn("Silent fail: Could not create internal audit log. User with ID {} not found.", userId);
            return;
        }

        AuditLog logEntity = new AuditLog(); // Renamed local variable 'log' to 'logEntity' to avoid collision with Lombok's 'log'
        logEntity.setUser(user);
        logEntity.setAction(action);
        logEntity.setEntityType(entityType);

        auditLogRepository.save(logEntity);
        log.info("Successfully saved internal audit log for user: {}, action: {}", userId, action);
    }

    // POST /api/audit — external trigger (Admin only)
    @Transactional
    public AuditLogResponse create(AuditLogRequest request) {
        log.info("Admin request to manually create audit log for userId: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.error("Failed to create manual audit log: User {} not found", request.getUserId());
                    return new ResourceNotFoundException("User not found");
                });

        AuditLog logEntity = new AuditLog(); // Renamed to avoid collision
        logEntity.setUser(user);
        logEntity.setAction(request.getAction());
        logEntity.setEntityType(request.getEntityType());

        AuditLogResponse response = toResponse(auditLogRepository.save(logEntity));
        log.info("Manual audit log created successfully with ID: {}", response.getAuditId());
        return response;
    }

    // GET /api/audit?userId=&entityType=&from=&to=
    public List<AuditLogResponse> query(String userId, String entityType,
                                        LocalDateTime from, LocalDateTime to) {

        log.info("Processing audit log query with filters -> userId: {}, entityType: {}, from: {}, to: {}",
                userId, entityType, from, to);

        if (userId != null) {
            log.debug("Querying audit logs by userId: {}", userId);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("Query failed: User {} not found", userId);
                        return new ResourceNotFoundException("User not found");
                    });
            return auditLogRepository.findByUserOrderByTimestampDesc(user)
                    .stream().map(this::toResponse).toList();
        }
        if (entityType != null) {
            log.debug("Querying audit logs by entityType: {}", entityType);
            return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType)
                    .stream().map(this::toResponse).toList();
        }
        if (from != null && to != null) {
            log.debug("Querying audit logs between dates: {} and {}", from, to);
            return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(from, to)
                    .stream().map(this::toResponse).toList();
        }

        log.debug("No filters provided. Fetching all audit logs.");
        return auditLogRepository.findAllByOrderByTimestampDesc()
                .stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .auditId(a.getAuditId())
                .userId(a.getUser().getUserId())
                .userName(a.getUser().getName())
                .userRole(a.getUser().getRole().name())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .timestamp(a.getTimestamp())
                .build();
    }
}