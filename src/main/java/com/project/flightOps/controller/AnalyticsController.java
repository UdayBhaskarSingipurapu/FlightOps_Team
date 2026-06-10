package com.project.flightOps.controller;

import com.project.flightOps.requestdto.ReportGenerateRequest;
import com.project.flightOps.responsedto.DashboardMetricsResponse;
import com.project.flightOps.responsedto.GroundOpsReportResponse;
import com.project.flightOps.service.AnalyticsService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ─── Live Metrics ────────────────────────────────────────────────────────────

    @GetMapping("/api/metrics/dashboard")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics fetched",
                analyticsService.getDashboard()));
    }

    @GetMapping("/api/metrics/on-time-rate")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOnTimeRate() {
        return ResponseEntity.ok(ApiResponse.success("On-time rate fetched",
                analyticsService.getOnTimeRate()));
    }

    // ─── Reports ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/reports/turnaround")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTurnaroundReport() {
        return ResponseEntity.ok(ApiResponse.success("Turnaround report fetched",
                analyticsService.getTurnaroundReport()));
    }

    @GetMapping("/api/reports/gse-utilisation")
    @PreAuthorize("hasAnyRole('Admin', 'GSEManager')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGseReport() {
        return ResponseEntity.ok(ApiResponse.success("GSE utilisation report fetched",
                analyticsService.getGseUtilisationReport()));
    }

    @GetMapping("/api/reports/baggage")
    @PreAuthorize("hasAnyRole('Admin', 'RampOfficer')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBaggageReport() {
        return ResponseEntity.ok(ApiResponse.success("Baggage report fetched",
                analyticsService.getBaggageReport()));
    }

    @GetMapping("/api/reports/sla-breaches")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSlaBreachReport() {
        return ResponseEntity.ok(ApiResponse.success("SLA breach report fetched",
                analyticsService.getSlaBreachReport()));
    }

    @PostMapping("/api/reports/generate")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<GroundOpsReportResponse>> generate(
            @Valid @RequestBody ReportGenerateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated",
                        analyticsService.generateReport(request)));
    }

    @GetMapping("/api/reports")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<List<GroundOpsReportResponse>>> getAllReports() {
        return ResponseEntity.ok(ApiResponse.success("Reports fetched",
                analyticsService.getAllReports()));
    }
}
