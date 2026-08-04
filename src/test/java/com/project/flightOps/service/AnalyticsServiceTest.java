package com.project.flightOps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.flightOps.entity.BaggageOperation;
import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.GroundEquipment;
import com.project.flightOps.entity.GroundOpsReport;
import com.project.flightOps.entity.TurnaroundMilestone;
import com.project.flightOps.entity.TurnaroundPlan;
import com.project.flightOps.enums.*;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.repository.*;
import com.project.flightOps.requestdto.ReportGenerateRequest;
import com.project.flightOps.responsedto.DashboardMetricsResponse;
import com.project.flightOps.responsedto.GroundOpsReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TurnaroundPlanRepository turnaroundPlanRepository;
    @Mock
    private TurnaroundMilestoneRepository milestoneRepository;
    @Mock
    private GroundEquipmentRepository equipmentRepository;
    @Mock
    private EquipmentAllocationRepository allocationRepository;
    @Mock
    private BaggageOperationRepository baggageOperationRepository;
    @Mock
    private MishandledBaggageRepository mishandledBaggageRepository;
    @Mock
    private SpecialAssistanceRepository assistanceRepository;
    @Mock
    private FlightRepository flightRepository;
    @Mock
    private GroundOpsReportRepository reportRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnalyticsService analyticsService;

    private TurnaroundPlan onTimeCompletedPlan;
    private TurnaroundPlan delayedPlan;
    private TurnaroundPlan activePlan;
    private BaggageOperation completedBaggageOp;
    private BaggageOperation discrepancyBaggageOp;
    private GroundEquipment availableEquipment;
    private GroundEquipment allocatedEquipment;
    private Flight flight;
    private GroundOpsReport groundOpsReport;
    private ReportGenerateRequest reportGenerateRequest;

    @BeforeEach
    void setUp() {
        onTimeCompletedPlan = new TurnaroundPlan();
        onTimeCompletedPlan.setPlanId("PLAN-1");
        onTimeCompletedPlan.setStatus(TurnaroundStatus.Completed);
        onTimeCompletedPlan.setTargetTurnaroundMinutes(45);
        onTimeCompletedPlan.setActualTurnaroundMinutes(40);

        delayedPlan = new TurnaroundPlan();
        delayedPlan.setPlanId("PLAN-2");
        delayedPlan.setStatus(TurnaroundStatus.Delayed);
        delayedPlan.setTargetTurnaroundMinutes(45);
        delayedPlan.setActualTurnaroundMinutes(60);

        activePlan = new TurnaroundPlan();
        activePlan.setPlanId("PLAN-3");
        activePlan.setStatus(TurnaroundStatus.Active);
        activePlan.setTargetTurnaroundMinutes(45);
        activePlan.setActualTurnaroundMinutes(null);

        completedBaggageOp = new BaggageOperation();
        completedBaggageOp.setOperationId("OP-1");
        completedBaggageOp.setStatus(OperationStatus.Completed);

        discrepancyBaggageOp = new BaggageOperation();
        discrepancyBaggageOp.setOperationId("OP-2");
        discrepancyBaggageOp.setStatus(OperationStatus.Discrepancy);

        availableEquipment = new GroundEquipment();
        availableEquipment.setEquipmentId("EQ-1");
        availableEquipment.setStatus(EquipmentStatus.Available);

        allocatedEquipment = new GroundEquipment();
        allocatedEquipment.setEquipmentId("EQ-2");
        allocatedEquipment.setStatus(EquipmentStatus.Allocated);

        flight = new Flight();
        flight.setFlightId("FL-1");
        flight.setAirlineCode("AA");
        flight.setFlightNumber("AA101");

        groundOpsReport = new GroundOpsReport();
        groundOpsReport.setReportId("REPORT-1");
        groundOpsReport.setScope("Airline:AA");
        groundOpsReport.setMetrics("{\"key\":\"value\"}");

        reportGenerateRequest = new ReportGenerateRequest();
        reportGenerateRequest.setScope("Airline:AA");
        reportGenerateRequest.setFromDate(LocalDate.now().minusDays(1));
        reportGenerateRequest.setToDate(LocalDate.now());
    }

    // --- getDashboard Tests ---

    @Test
    void getDashboard_Success_WithMixedData() {
        when(turnaroundPlanRepository.findAll())
                .thenReturn(List.of(onTimeCompletedPlan, delayedPlan, activePlan));
        when(equipmentRepository.count()).thenReturn(2L);
        when(equipmentRepository.findByStatus(EquipmentStatus.Allocated)).thenReturn(List.of(allocatedEquipment));
        when(baggageOperationRepository.findAll())
                .thenReturn(List.of(completedBaggageOp, discrepancyBaggageOp));
        when(milestoneRepository.findByStatusOrderByPlannedTimeAsc(MilestoneStatus.Delayed))
                .thenReturn(List.of(new TurnaroundMilestone()));
        when(mishandledBaggageRepository.findByStatus(MishandledStatus.Reported))
                .thenReturn(Collections.emptyList());
        when(assistanceRepository.findByStatusOrderByStatusAsc(AssistanceStatus.Requested))
                .thenReturn(Collections.emptyList());
        when(flightRepository.findByScheduledArrivalBetweenOrderByScheduledArrivalAsc(any(), any()))
                .thenReturn(List.of(flight));

        DashboardMetricsResponse response = analyticsService.getDashboard();

        assertNotNull(response);
        assertEquals(LocalDate.now(), response.getDate());
        assertEquals(1, response.getTotalFlightsHandled());
        assertEquals(1, response.getOnTimeTurnarounds());
        assertEquals(1, response.getDelayedTurnarounds());
        assertEquals(2, response.getTotalEquipment());
        assertEquals(1, response.getAllocatedEquipment());
        assertEquals(50.0, response.getGseUtilisationPercent());
        assertEquals(2, response.getTotalBaggageOps());
        assertEquals(1, response.getDiscrepancyOps());
        assertEquals(50.0, response.getBaggageDiscrepancyRatePercent());
        assertEquals(1, response.getSlaBreachCount());
        assertEquals(0, response.getMishandledBagsReported());
        assertEquals(0, response.getOpenAssistanceRequests());
    }

    @Test
    void getDashboard_Success_WithNoDataReturnsZeroSafeRates() {
        when(turnaroundPlanRepository.findAll()).thenReturn(Collections.emptyList());
        when(equipmentRepository.count()).thenReturn(0L);
        when(equipmentRepository.findByStatus(EquipmentStatus.Allocated)).thenReturn(Collections.emptyList());
        when(baggageOperationRepository.findAll()).thenReturn(Collections.emptyList());
        when(milestoneRepository.findByStatusOrderByPlannedTimeAsc(MilestoneStatus.Delayed))
                .thenReturn(Collections.emptyList());
        when(mishandledBaggageRepository.findByStatus(MishandledStatus.Reported))
                .thenReturn(Collections.emptyList());
        when(assistanceRepository.findByStatusOrderByStatusAsc(AssistanceStatus.Requested))
                .thenReturn(Collections.emptyList());
        when(flightRepository.findByScheduledArrivalBetweenOrderByScheduledArrivalAsc(any(), any()))
                .thenReturn(Collections.emptyList());

        DashboardMetricsResponse response = analyticsService.getDashboard();

        assertNotNull(response);
        assertEquals(0, response.getTotalFlightsHandled());
        assertEquals(0.0, response.getOnTimeRatePercent());
        assertEquals(0.0, response.getAvgTurnaroundMinutes());
        assertEquals(0.0, response.getGseUtilisationPercent());
        assertEquals(0.0, response.getBaggageDiscrepancyRatePercent());
        assertEquals(0, response.getSlaBreachCount());
        assertEquals(0, response.getMishandledBagsReported());
        assertEquals(0, response.getOpenAssistanceRequests());
    }

    // --- getOnTimeRate Tests ---

    @Test
    void getOnTimeRate_Success_CalculatesRate() {
        when(turnaroundPlanRepository.findAll())
                .thenReturn(List.of(onTimeCompletedPlan, delayedPlan));

        Map<String, Object> result = analyticsService.getOnTimeRate();

        assertNotNull(result);
        assertEquals(2L, result.get("totalTurnarounds"));
        assertEquals(1L, result.get("onTimeTurnarounds"));
        assertEquals(50.0, result.get("onTimeRatePercent"));
    }
}