package com.project.flightOps.controller;

import com.project.flightOps.requestdto.AuditLogRequest;
import com.project.flightOps.responsedto.AuditLogResponse;
import com.project.flightOps.service.AuditLogService;
import com.project.flightOps.util.ApiResponse;
import com.project.flightOps.util.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 1. Imported Lombok's Slf4j logger
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('Admin')")
@RequiredArgsConstructor
@Slf4j // 2. Added annotation to automatically inject the 'log' object
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<ApiResponse<AuditLogResponse>> create(
            @Valid @RequestBody AuditLogRequest request) {

        // Log the attempt to create a record using parameterized anchors '{}'
        log.info("REST request to record a new audit log. Action: {}, EntityType: {}, PerformedBy: {}",
                request.getAction(), request.getEntityType(), request.getUserId());

        AuditLogResponse response = auditLogService.create(request);

        log.debug("Audit log successfully written to database with ID: {}", response.getAuditId());
        return ResponseEntity.ok(ApiResponse.success("Audit log recorded", response));
    }

    // GET /api/audit?userId=&entityType=&from=2025-01-01T00:00:00&to=2025-01-31T23:59:59&page=1&limit=10
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> query(
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        // Log incoming search parameters safely
        log.info("REST request to query system audit logs. Filter criteria -> UserEmail: {}, EntityType: {}, From: {}, To: {}, Page: {}, Limit: {}",
                userEmail, entityType, from, to, page, limit);

        PageResponse<AuditLogResponse> logs = auditLogService.query(userEmail, entityType, from, to, page, limit);

        log.info("Successfully fetched {} of {} audit log entries matching criteria.", logs.getData().size(), logs.getTotalCount());
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched", logs));
    }
}