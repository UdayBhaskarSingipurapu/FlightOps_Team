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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    // Called internally (e.g., after login, after turnaround complete, after status change)
    @Transactional
    public void log(String userId, String action, String entityType) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return; // silent fail — don't break business flow for audit
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        auditLogRepository.save(log);
    }

    // POST /api/audit — external trigger (Admin only)
    @Transactional
    public AuditLogResponse create(AuditLogRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(request.getAction());
        log.setEntityType(request.getEntityType());
        return toResponse(auditLogRepository.save(log));
    }

    // GET /api/audit?userId=&entityType=&from=&to=
    public List<AuditLogResponse> query(String userId, String entityType,
            LocalDateTime from, LocalDateTime to) {

        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return auditLogRepository.findByUserOrderByTimestampDesc(user)
                    .stream().map(this::toResponse).toList();
        }
        if (entityType != null) {
            return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType)
                    .stream().map(this::toResponse).toList();
        }
        if (from != null && to != null) {
            return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(from, to)
                    .stream().map(this::toResponse).toList();
        }
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
