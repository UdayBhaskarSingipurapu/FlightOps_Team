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


}