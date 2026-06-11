package com.project.flightOps.controller;

import com.project.flightOps.requestdto.ReportGenerateRequest;
import com.project.flightOps.responsedto.DashboardMetricsResponse;
import com.project.flightOps.responsedto.GroundOpsReportResponse;
import com.project.flightOps.service.AnalyticsService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 1. Imported Lombok's Slf4j
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
@Slf4j // 2. Added annotation to automatically create the 'log' instance
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ─── Live Metrics ────────────────────────────────────────────────────────────

    @GetMapping("/api/metrics/dashboard")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboard() {
        log.info("REST request to fetch live dashboard metrics.");
        DashboardMetricsResponse response = analyticsService.getDashboard();
        log.debug("Successfully retrieved dashboard metrics data.");
        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics fetched", response));
    }

    @GetMapping("/api/metrics/on-time-rate")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOnTimeRate() {
        log.info("REST request to fetch flight on-time performance rates.");
        Map<String, Object> onTimeData = analyticsService.getOnTimeRate();
        return ResponseEntity.ok(ApiResponse.success("On-time rate fetched", onTimeData));
    }

    // ─── Reports ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/reports/turnaround")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTurnaroundReport() {
        log.info("REST request to fetch comprehensive flight turnaround report.");
        Map<String, Object> report = analyticsService.getTurnaroundReport();
        return ResponseEntity.ok(ApiResponse.success("Turnaround report fetched", report));
    }

    @GetMapping("/api/reports/gse-utilisation")
    @PreAuthorize("hasAnyRole('Admin', 'GSEManager')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGseReport() {
        log.info("REST request to fetch Ground Support Equipment (GSE) utilisation metrics.");
        Map<String, Object> gseData = analyticsService.getGseUtilisationReport();
        return ResponseEntity.ok(ApiResponse.success("GSE utilisation report fetched", gseData));
    }

    @GetMapping("/api/reports/baggage")
    @PreAuthorize("hasAnyRole('Admin', 'RampOfficer')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBaggageReport() {
        log.info("REST request to fetch ramp baggage handling report.");
        Map<String, Object> baggageData = analyticsService.getBaggageReport();
        return ResponseEntity.ok(ApiResponse.success("Baggage report fetched", baggageData));
    }

    @GetMapping("/api/reports/sla-breaches")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSlaBreachReport() {
        log.warn("REST request to inspect ground operations SLA breaches."); // Using WARN level for visibility on breaches
        Map<String, Object> slaData = analyticsService.getSlaBreachReport();
        return ResponseEntity.ok(ApiResponse.success("SLA breach report fetched", slaData));
    }

    @PostMapping("/api/reports/generate")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<GroundOpsReportResponse>> generate(
            @Valid @RequestBody ReportGenerateRequest request) {

        // Using {} placeholder styling to safely inject variables without manual string concatenation
        log.info("REST request to generate a new custom report. Type: {}, Range: {}",
                request.getScope(), request.getFromDate());

        long startTime = System.currentTimeMillis();
        GroundOpsReportResponse generatedReport = analyticsService.generateReport(request);
        long duration = System.currentTimeMillis() - startTime;

        log.info("Successfully generated report ID: {} in {} ms", generatedReport.getReportId(), duration);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated", generatedReport));
    }

    @GetMapping("/api/reports")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<List<GroundOpsReportResponse>>> getAllReports() {
        log.info("REST request to fetch historical archive list of all generated reports.");
        List<GroundOpsReportResponse> reportsList = analyticsService.getAllReports();
        log.debug("Total reports extracted from archive storage count: {}", reportsList.size());
        return ResponseEntity.ok(ApiResponse.success("Reports fetched", reportsList));
    }
}