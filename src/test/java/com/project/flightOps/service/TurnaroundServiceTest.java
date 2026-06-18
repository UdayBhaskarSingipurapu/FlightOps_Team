package com.project.flightOps.service;

import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.TurnaroundMilestone;
import com.project.flightOps.entity.TurnaroundPlan;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.*;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.TurnaroundMilestoneRepository;
import com.project.flightOps.repository.TurnaroundPlanRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.MilestoneCompleteRequest;
import com.project.flightOps.requestdto.TurnaroundPlanRequest;
import com.project.flightOps.responsedto.TurnaroundMilestoneResponse;
import com.project.flightOps.responsedto.TurnaroundPlanResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnaroundServiceTest {

    @Mock
    private TurnaroundPlanRepository planRepository;
    @Mock
    private TurnaroundMilestoneRepository milestoneRepository;
    @Mock
    private FlightService flightService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TurnaroundService turnaroundService;

    private Flight mockFlight;
    private User mockSupervisor;
    private User mockRampOfficer;
    private TurnaroundPlan mockPlan;
    private TurnaroundMilestone mockChocksOnMilestone;
    private TurnaroundMilestone mockPushbackMilestone;

    private final String flightId = "FL-999";
    private final String supervisorEmail = "sup@airport.com";
    private final String supervisorId = "USR-SUP";
    private final String planId = "PLAN-123";
    private final String milestoneId = "MS-001";

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        mockFlight = new Flight();
        mockFlight.setFlightId(flightId);
        mockFlight.setFlightNumber("AA505");
        mockFlight.setScheduledArrival(now);
        mockFlight.setStand("Gate 4");

        mockSupervisor = new User();
        mockSupervisor.setUserId(supervisorId);
        mockSupervisor.setEmail(supervisorEmail);
        mockSupervisor.setName("John Supervisor");

        mockRampOfficer = new User();
        mockRampOfficer.setUserId("USR-RAMP");

        mockPlan = new TurnaroundPlan();
        mockPlan.setPlanId(planId);
        mockPlan.setFlight(mockFlight);
        mockPlan.setSupervisor(mockSupervisor);
        mockPlan.setTargetTurnaroundMinutes(60);
        mockPlan.setStatus(TurnaroundStatus.Active);

        mockChocksOnMilestone = new TurnaroundMilestone();
        mockChocksOnMilestone.setMilestoneId(milestoneId);
        mockChocksOnMilestone.setTurnaroundPlan(mockPlan);
        mockChocksOnMilestone.setMilestoneType(MilestoneType.ChocksOn);
        mockChocksOnMilestone.setPlannedTime(now.plusMinutes(2));
        mockChocksOnMilestone.setStatus(MilestoneStatus.Pending);

        mockPushbackMilestone = new TurnaroundMilestone();
        mockPushbackMilestone.setMilestoneId("MS-010");
        mockPushbackMilestone.setTurnaroundPlan(mockPlan);
        mockPushbackMilestone.setMilestoneType(MilestoneType.PushbackClearance);
        mockPushbackMilestone.setPlannedTime(now.plusMinutes(60));
        mockPushbackMilestone.setStatus(MilestoneStatus.Pending);
    }

    // --- createPlan Tests ---

    @Test
    void createPlan_Success() {
        TurnaroundPlanRequest request = new TurnaroundPlanRequest();
        request.setFlightId(flightId);
        request.setTargetTurnaroundMinutes(60);

        when(flightService.findById(flightId)).thenReturn(mockFlight);
        when(planRepository.existsByFlight(mockFlight)).thenReturn(false);
        when(userRepository.findByEmail(supervisorEmail)).thenReturn(Optional.of(mockSupervisor));
        when(planRepository.save(any(TurnaroundPlan.class))).thenReturn(mockPlan);
        when(userRepository.findByRole(Role.RampOfficer)).thenReturn(List.of(mockRampOfficer));

        TurnaroundPlanResponse response = turnaroundService.createPlan(request, supervisorEmail);

        assertNotNull(response);
        assertEquals(planId, response.getPlanId());
        verify(milestoneRepository).saveAll(anyList()); // Verifies all 10 milestones get saved
        verify(notificationService).sendNotification(eq("USR-RAMP"), anyString(), eq(NotificationCategory.Turnaround));
    }

    @Test
    void createPlan_ThrowsConflictException_WhenPlanExists() {
        TurnaroundPlanRequest request = new TurnaroundPlanRequest();
        request.setFlightId(flightId);

        when(flightService.findById(flightId)).thenReturn(mockFlight);
        when(planRepository.existsByFlight(mockFlight)).thenReturn(true);

        assertThrows(ConflictException.class, () -> turnaroundService.createPlan(request, supervisorEmail));
        verify(planRepository, never()).save(any());
    }

    @Test
    void createPlan_ThrowsResourceNotFoundException_WhenSupervisorMissing() {
        TurnaroundPlanRequest request = new TurnaroundPlanRequest();
        request.setFlightId(flightId);

        when(flightService.findById(flightId)).thenReturn(mockFlight);
        when(planRepository.existsByFlight(mockFlight)).thenReturn(false);
        when(userRepository.findByEmail(supervisorEmail)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> turnaroundService.createPlan(request, supervisorEmail));
    }

    // --- completePlan Tests ---

    @Test
    void completePlan_Success_CalculatesActualDuration() {
        LocalDateTime arrival = LocalDateTime.now();
        mockChocksOnMilestone.setActualTime(arrival);
        mockPushbackMilestone.setActualTime(arrival.plusMinutes(55)); // Completed in 55 mins

        when(planRepository.findById(planId)).thenReturn(Optional.of(mockPlan));
        when(milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(mockPlan))
                .thenReturn(List.of(mockChocksOnMilestone, mockPushbackMilestone));
        when(planRepository.save(any(TurnaroundPlan.class))).thenReturn(mockPlan);

        TurnaroundPlanResponse response = turnaroundService.completePlan(planId);

        assertNotNull(response);
        assertEquals(TurnaroundStatus.Completed, mockPlan.getStatus());
        assertEquals(55, mockPlan.getActualTurnaroundMinutes());
        verify(notificationService).sendNotification(eq(supervisorId), anyString(), eq(NotificationCategory.Turnaround));
    }

    @Test
    void completePlan_ThrowsBadRequestException_WhenAlreadyCompleted() {
        mockPlan.setStatus(TurnaroundStatus.Completed);
        when(planRepository.findById(planId)).thenReturn(Optional.of(mockPlan));

        assertThrows(BadRequestException.class, () -> turnaroundService.completePlan(planId));
        verify(planRepository, never()).save(any());
    }

    // --- completeMilestone Tests ---

    @Test
    void completeMilestone_Success_OnTime() {
        MilestoneCompleteRequest request = new MilestoneCompleteRequest();
        // Planned is now + 2 mins, so actual at now + 1 min is On-Time
        request.setActualTime(mockChocksOnMilestone.getPlannedTime().minusMinutes(1));

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(mockChocksOnMilestone));
        when(userRepository.findByEmail(supervisorEmail)).thenReturn(Optional.of(mockSupervisor));
        when(milestoneRepository.save(any(TurnaroundMilestone.class))).thenReturn(mockChocksOnMilestone);

        TurnaroundMilestoneResponse response = turnaroundService.completeMilestone(milestoneId, request, supervisorEmail);

        assertNotNull(response);
        assertEquals(MilestoneStatus.Completed, mockChocksOnMilestone.getStatus());
        assertFalse(response.isDelayed());
        verify(notificationService, never()).sendNotification(any(), any(), any());
    }

    @Test
    void completeMilestone_Success_DelayedBreachesSLA() {
        MilestoneCompleteRequest request = new MilestoneCompleteRequest();
        // Planned is now + 2 mins, completing at + 12 mins triggers 10 mins delay
        request.setActualTime(mockChocksOnMilestone.getPlannedTime().plusMinutes(10));

        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(mockChocksOnMilestone));
        when(userRepository.findByEmail(supervisorEmail)).thenReturn(Optional.of(mockSupervisor));
        when(milestoneRepository.save(any(TurnaroundMilestone.class))).thenReturn(mockChocksOnMilestone);

        TurnaroundMilestoneResponse response = turnaroundService.completeMilestone(milestoneId, request, supervisorEmail);

        assertNotNull(response);
        assertEquals(MilestoneStatus.Delayed, mockChocksOnMilestone.getStatus());
        assertEquals(TurnaroundStatus.Delayed, mockPlan.getStatus()); // Plan gets downgraded to delayed
        assertTrue(response.isDelayed());
        assertEquals(10L, response.getDelayMinutes());
        // Verifies SLA breach alert notification sent to Supervisor
        verify(notificationService).sendNotification(eq(supervisorId), contains("SLA breach"), eq(NotificationCategory.Turnaround));
    }

    @Test
    void completeMilestone_ThrowsBadRequestException_WhenMilestoneAlreadyComplete() {
        mockChocksOnMilestone.setStatus(MilestoneStatus.Completed);
        when(milestoneRepository.findById(milestoneId)).thenReturn(Optional.of(mockChocksOnMilestone));

        assertThrows(BadRequestException.class, () ->
                turnaroundService.completeMilestone(milestoneId, new MilestoneCompleteRequest(), supervisorEmail));
    }

    // --- checkOverdueMilestones (Scheduled Task) Tests ---

    @Test
    void checkOverdueMilestones_SendsAlertsForOverdueItems() {
        mockChocksOnMilestone.setPlannedTime(LocalDateTime.now().minusMinutes(15)); // 15 mins overdue
        when(milestoneRepository.findOverdueMilestones(any(LocalDateTime.now().getClass()))).thenReturn(List.of(mockChocksOnMilestone));

        turnaroundService.checkOverdueMilestones();

        verify(notificationService).sendNotification(eq(supervisorId), contains("OVERDUE:"), eq(NotificationCategory.Turnaround));
    }

    @Test
    void checkOverdueMilestones_NoActionWhenEmpty() {
        when(milestoneRepository.findOverdueMilestones(any())).thenReturn(new ArrayList<>());

        turnaroundService.checkOverdueMilestones();

        verify(notificationService, never()).sendNotification(any(), any(), any());
    }

    // --- Read operations Tests ---

    @Test
    void getByFlight_Success() {
        when(planRepository.findByFlight_FlightId(flightId)).thenReturn(Optional.of(mockPlan));
        when(milestoneRepository.findByTurnaroundPlanOrderByPlannedTimeAsc(mockPlan)).thenReturn(List.of(mockChocksOnMilestone));

        TurnaroundPlanResponse response = turnaroundService.getByFlight(flightId);

        assertNotNull(response);
        assertEquals(flightId, response.getFlightId());
    }

    @Test
    void getByFlight_ThrowsResourceNotFoundException() {
        when(planRepository.findByFlight_FlightId(flightId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> turnaroundService.getByFlight(flightId));
    }
}