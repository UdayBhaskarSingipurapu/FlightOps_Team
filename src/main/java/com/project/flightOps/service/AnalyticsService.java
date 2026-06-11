package com.project.flightOps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.flightOps.entity.GroundOpsReport;
import com.project.flightOps.enums.*;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.repository.*;
import com.project.flightOps.requestdto.ReportGenerateRequest;
import com.project.flightOps.responsedto.DashboardMetricsResponse;
import com.project.flightOps.responsedto.GroundOpsReportResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for logger
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j // Injects `log` instance automatically via Lombok
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TurnaroundPlanRepository turnaroundPlanRepository;
    private final TurnaroundMilestoneRepository milestoneRepository;
    private final GroundEquipmentRepository equipmentRepository;
    private final EquipmentAllocationRepository allocationRepository;
    private final BaggageOperationRepository baggageOperationRepository;
    private final MishandledBaggageRepository mishandledBaggageRepository;
    private final SpecialAssistanceRepository assistanceRepository;
    private final FlightRepository flightRepository;
    private final GroundOpsReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    // GET /api/metrics/dashboard — today's live KPIs
    public DashboardMetricsResponse getDashboard() {
        LocalDate today = LocalDate.now();
        log.info("Fetching real-time dashboard metrics for date: {}", today);

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        // Turnaround metrics
        var allPlans = turnaroundPlanRepository.findAll();
        long total = allPlans.size();
        long onTime = allPlans.stream()
                .filter(p -> p.getStatus() == TurnaroundStatus.Completed
                        && p.getActualTurnaroundMinutes() != null
                        && p.getTargetTurnaroundMinutes() != null
                        && p.getActualTurnaroundMinutes() <= p.getTargetTurnaroundMinutes())
                .count();
        long delayed = allPlans.stream()
                .filter(p -> p.getStatus() == TurnaroundStatus.Delayed).count();

        double avgActual = allPlans.stream()
                .filter(p -> p.getActualTurnaroundMinutes() != null)
                .mapToInt(p -> p.getActualTurnaroundMinutes())
                .average().orElse(0.0);

        double onTimeRate = total > 0 ? (onTime * 100.0 / total) : 0.0;
        log.debug("Turnaround calculations completed. Total: {}, On-Time: {}, Delayed: {}", total, onTime, delayed);

        // GSE utilisation
        long totalEquip = equipmentRepository.count();
        long allocated = equipmentRepository.findByStatus(EquipmentStatus.Allocated).size();
        double gseUtil = totalEquip > 0 ? (allocated * 100.0 / totalEquip) : 0.0;

        // Baggage metrics
        var allOps = baggageOperationRepository.findAll();
        long discrepancyOps = allOps.stream()
                .filter(op -> op.getStatus() == OperationStatus.Discrepancy).count();
        double discRate = allOps.isEmpty() ? 0.0
                : (discrepancyOps * 100.0 / allOps.size());

        // SLA breaches = delayed milestones
        long slaBreaches = milestoneRepository
                .findByStatusOrderByPlannedTimeAsc(MilestoneStatus.Delayed).size();

        // Mishandled bags
        long mishandled = mishandledBaggageRepository.findByStatus(MishandledStatus.Reported).size();

        // Open assistance requests
        long openAssistance = assistanceRepository
                .findByStatusOrderByStatusAsc(AssistanceStatus.Requested).size();

        // Today's flights
        long flightsHandled = flightRepository
                .findByScheduledArrivalBetweenOrderByScheduledArrivalAsc(startOfDay, endOfDay).size();

        log.info("Successfully compiled dashboard metrics. Flights handled: {}, SLA breaches: {}", flightsHandled, slaBreaches);

        return DashboardMetricsResponse.builder()
                .date(today)
                .totalFlightsHandled((int) flightsHandled)
                .onTimeTurnarounds((int) onTime)
                .delayedTurnarounds((int) delayed)
                .onTimeRatePercent(Math.round(onTimeRate * 10.0) / 10.0)
                .avgTurnaroundMinutes(Math.round(avgActual * 10.0) / 10.0)
                .totalEquipment((int) totalEquip)
                .allocatedEquipment((int) allocated)
                .gseUtilisationPercent(Math.round(gseUtil * 10.0) / 10.0)
                .totalBaggageOps(allOps.size())
                .discrepancyOps((int) discrepancyOps)
                .baggageDiscrepancyRatePercent(Math.round(discRate * 10.0) / 10.0)
                .slaBreachCount((int) slaBreaches)
                .mishandledBagsReported((int) mishandled)
                .openAssistanceRequests((int) openAssistance)
                .build();
    }

    // GET /api/metrics/on-time-rate
    public Map<String, Object> getOnTimeRate() {
        log.info("Calculating On-Time Performance (OTP) rate...");
        var allPlans = turnaroundPlanRepository.findAll();
        long total = allPlans.size();
        long onTime = allPlans.stream()
                .filter(p -> p.getStatus() == TurnaroundStatus.Completed
                        && p.getActualTurnaroundMinutes() != null
                        && p.getTargetTurnaroundMinutes() != null
                        && p.getActualTurnaroundMinutes() <= p.getTargetTurnaroundMinutes())
                .count();
        double rate = total > 0 ? (onTime * 100.0 / total) : 0.0;

        log.info("OTP generation finished. Rate: {}%", Math.round(rate * 10.0) / 10.0);
        return Map.of(
                "totalTurnarounds", total,
                "onTimeTurnarounds", onTime,
                "onTimeRatePercent", Math.round(rate * 10.0) / 10.0
        );
    }

    // GET /api/reports/turnaround
    public Map<String, Object> getTurnaroundReport() {
        log.info("Generating full Turnaround operational report...");
        var plans = turnaroundPlanRepository.findAll();
        long completed = plans.stream()
                .filter(p -> p.getStatus() == TurnaroundStatus.Completed).count();
        long active = plans.stream()
                .filter(p -> p.getStatus() == TurnaroundStatus.Active).count();
        long delayed = plans.stream()
                .filter(p -> p.getStatus() == TurnaroundStatus.Delayed).count();
        double avg = plans.stream()
                .filter(p -> p.getActualTurnaroundMinutes() != null)
                .mapToInt(p -> p.getActualTurnaroundMinutes())
                .average().orElse(0.0);

        return Map.of(
                "total", plans.size(),
                "completed", completed,
                "active", active,
                "delayed", delayed,
                "avgActualMinutes", Math.round(avg * 10.0) / 10.0
        );
    }

    // GET /api/reports/gse-utilisation
    public Map<String, Object> getGseUtilisationReport() {
        log.info("Generating Ground Support Equipment (GSE) utilization report...");
        long total = equipmentRepository.count();
        long available = equipmentRepository.findByStatus(EquipmentStatus.Available).size();
        long allocated = equipmentRepository.findByStatus(EquipmentStatus.Allocated).size();
        long maintenance = equipmentRepository.findByStatus(EquipmentStatus.Maintenance).size();
        long outOfService = equipmentRepository.findByStatus(EquipmentStatus.OutOfService).size();
        double utilRate = total > 0 ? (allocated * 100.0 / total) : 0.0;

        return Map.of(
                "totalEquipment", total,
                "available", available,
                "allocated", allocated,
                "inMaintenance", maintenance,
                "outOfService", outOfService,
                "utilisationPercent", Math.round(utilRate * 10.0) / 10.0
        );
    }

    // GET /api/reports/baggage
    public Map<String, Object> getBaggageReport() {
        log.info("Generating Baggage operations report...");
        var allOps = baggageOperationRepository.findAll();
        long discrepancy = allOps.stream()
                .filter(op -> op.getStatus() == OperationStatus.Discrepancy).count();
        long completed = allOps.stream()
                .filter(op -> op.getStatus() == OperationStatus.Completed).count();
        long mishandled = mishandledBaggageRepository.count();
        double discRate = allOps.isEmpty() ? 0.0 : (discrepancy * 100.0 / allOps.size());

        return Map.of(
                "totalOperations", allOps.size(),
                "completedOperations", completed,
                "discrepancyOperations", discrepancy,
                "discrepancyRatePercent", Math.round(discRate * 10.0) / 10.0,
                "totalMishandledCases", mishandled
        );
    }

    // GET /api/reports/sla-breaches
    public Map<String, Object> getSlaBreachReport() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Evaluating SLA breaches up to timestamp: {}", now);

        long delayed = milestoneRepository
                .findByStatusOrderByPlannedTimeAsc(MilestoneStatus.Delayed).size();
        long pending = milestoneRepository
                .findByStatusOrderByPlannedTimeAsc(MilestoneStatus.Pending).size();
        long overdue = milestoneRepository
                .findOverdueMilestones(now).size();

        return Map.of(
                "completedOnTime", milestoneRepository
                        .findByStatusOrderByPlannedTimeAsc(MilestoneStatus.Completed).size(),
                "delayedMilestones", delayed,
                "pendingMilestones", pending,
                "currentlyOverdue", overdue,
                "totalSlaBreaches", delayed + overdue
        );
    }

    // POST /api/reports/generate — saves a snapshot report to DB
    @Transactional
    public GroundOpsReportResponse generateReport(ReportGenerateRequest request) {
        log.info("Request received to generate snapshot report. Scope: {}, DateRange: {} to {}",
                request.getScope(), request.getFromDate(), request.getToDate());

        if (request.getToDate().isBefore(request.getFromDate())) {
            log.warn("Report generation validation failed: 'toDate' ({}) is before 'fromDate' ({})",
                    request.getToDate(), request.getFromDate());
            throw new BadRequestException("To date must be after from date");
        }

        // Build metrics snapshot
        DashboardMetricsResponse metrics = getDashboard();
        String metricsJson;
        try {
            metricsJson = objectMapper.writeValueAsString(metrics);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metrics to JSON string for database snapshot preservation", e);
            metricsJson = "{\"error\": \"Could not serialize metrics\"}";
        }

        GroundOpsReport report = new GroundOpsReport();
        report.setScope(request.getScope());
        report.setMetrics(metricsJson);

        GroundOpsReport savedReport = reportRepository.save(report);
        log.info("Successfully generated and preserved ground operations report snapshot with ID: {}", savedReport.getReportId());

        return toReportResponse(savedReport);
    }

    // GET /api/reports — list saved reports
    public List<GroundOpsReportResponse> getAllReports() {
        log.info("Fetching historic list of all saved reports...");
        return reportRepository.findAllByOrderByGeneratedDateDesc()
                .stream().map(this::toReportResponse).toList();
    }

    private GroundOpsReportResponse toReportResponse(GroundOpsReport r) {
        return GroundOpsReportResponse.builder()
                .reportId(r.getReportId())
                .scope(r.getScope())
                .metrics(r.getMetrics())
                .generatedDate(r.getGeneratedDate())
                .build();
    }
}