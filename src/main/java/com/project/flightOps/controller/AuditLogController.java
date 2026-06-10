package com.project.flightOps.controller;

import com.project.flightOps.requestdto.AuditLogRequest;
import com.project.flightOps.responsedto.AuditLogResponse;
import com.project.flightOps.service.AuditLogService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('Admin')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<ApiResponse<AuditLogResponse>> create(
            @Valid @RequestBody AuditLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Audit log recorded",
                auditLogService.create(request)));
    }

    // GET /api/audit?userId=&entityType=&from=2025-01-01T00:00:00&to=2025-01-31T23:59:59
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> query(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched",
                auditLogService.query(userId, entityType, from, to)));
    }
}
