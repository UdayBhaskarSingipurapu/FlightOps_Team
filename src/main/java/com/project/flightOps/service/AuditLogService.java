package com.project.flightOps.service;

import com.project.flightOps.entity.AuditLog;
import com.project.flightOps.entity.User;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.AuditLogRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.AuditLogRequest;
import com.project.flightOps.responsedto.AuditLogResponse;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 1. Added SLF4J Import
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public List<AuditLogResponse> query(String userEmail, String entityType,
                                        LocalDateTime from, LocalDateTime to) {

        log.info("Processing combined audit log query with filters -> userEmail: {}, entityType: {}, from: {}, to: {}",
                userEmail, entityType, from, to);

        Specification<AuditLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userEmail != null && !userEmail.trim().isEmpty()) {
                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
                predicates.add(criteriaBuilder.equal(root.get("user"), user));
            }

            if (entityType != null && !entityType.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("entityType"), entityType));
            }

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), from));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), to));
            }

            // Enforce the sorting order by timestamp descending
            query.orderBy(criteriaBuilder.desc(root.get("timestamp")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .auditId(a.getAuditId())
                .userEmail(a.getUser().getEmail())
                .userName(a.getUser().getName())
                .userRole(a.getUser().getRole().name())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .timestamp(a.getTimestamp())
                .build();
    }
}