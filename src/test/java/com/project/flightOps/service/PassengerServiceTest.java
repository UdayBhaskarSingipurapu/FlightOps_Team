package com.project.flightOps.service;

import com.project.flightOps.entity.BoardingGate;
import com.project.flightOps.entity.CheckInCounter;
import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.SpecialAssistance;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.*;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.BoardingGateRepository;
import com.project.flightOps.repository.CheckInCounterRepository;
import com.project.flightOps.repository.SpecialAssistanceRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.*;
import com.project.flightOps.responsedto.BoardingGateResponse;
import com.project.flightOps.responsedto.CheckInCounterResponse;
import com.project.flightOps.responsedto.SpecialAssistanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerServiceTest {

    @Mock
    private CheckInCounterRepository counterRepository;
    @Mock
    private BoardingGateRepository gateRepository;
    @Mock
    private SpecialAssistanceRepository assistanceRepository;
    @Mock
    private FlightService flightService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PassengerService passengerService;

    private Flight flight;
    private User agent;
    private User currentUser;

    private final String flightId = "FL-100";
    private final String agentId = "USR-AGENT";
    private final String currentUserId = "USR-CURRENT";
    private final String currentUserEmail = "current@airport.com";

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        flight = new Flight();
        flight.setFlightId(flightId);
        flight.setFlightNumber("AA202");
        flight.setScheduledArrival(now);
        flight.setScheduledDeparture(now.plusHours(2));

        agent = new User();
        agent.setUserId(agentId);
        agent.setName("Agent Smith");
        agent.setEmail("agent@airport.com");
        agent.setRole(Role.PassengerAgent);

        currentUser = new User();
        currentUser.setUserId(currentUserId);
        currentUser.setName("Current User");
        currentUser.setEmail(currentUserEmail);
        currentUser.setRole(Role.PassengerAgent);
    }

    private MockedStatic<SecurityContextHolder> mockSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class);
        mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        return mockedStatic;
    }

    // --- assignCounter Tests ---

    @Test
    void assignCounter_Success_WithAgent() {
        CheckInCounterRequest request = new CheckInCounterRequest();
        request.setCounterNumber("C1");
        request.setTerminal("T1");
        request.setFlightId(flightId);
        request.setAssignedAgentId(agentId);
        request.setOpenTime(LocalDateTime.now());
        request.setCloseTime(LocalDateTime.now().plusHours(3));

        when(counterRepository.existsByCounterNumberAndStatusNot("C1", CounterStatus.Closed)).thenReturn(false);
        when(flightService.findById(flightId)).thenReturn(flight);
        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(counterRepository.save(any(CheckInCounter.class))).thenAnswer(inv -> {
            CheckInCounter c = inv.getArgument(0);
            c.setCounterId("CTR-1");
            return c;
        });
        when(userRepository.findByRole(Role.AirlineCoordinator)).thenReturn(List.of());

        CheckInCounterResponse response = passengerService.assignCounter(request);

        assertNotNull(response);
        assertEquals("CTR-1", response.getCounterId());
        assertEquals(CounterStatus.Standby, response.getStatus());
        assertEquals(agentId, response.getAssignedAgentId());
        verify(auditLogService).log(eq(agentId), eq("ASSIGNED_CHECK_IN_COUNTER"), eq("CheckInCounter"));
        verify(counterRepository).save(any(CheckInCounter.class));
    }

    @Test
    void assignCounter_Success_WithoutAgent_UsesCurrentUser() {
        CheckInCounterRequest request = new CheckInCounterRequest();
        request.setCounterNumber("C2");
        request.setTerminal("T1");
        request.setFlightId(flightId);
        request.setOpenTime(LocalDateTime.now());

        when(counterRepository.existsByCounterNumberAndStatusNot("C2", CounterStatus.Closed)).thenReturn(false);
        when(flightService.findById(flightId)).thenReturn(flight);
        when(counterRepository.save(any(CheckInCounter.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRole(Role.AirlineCoordinator)).thenReturn(List.of());

        try (MockedStatic<SecurityContextHolder> ignored = mockSecurityContext(currentUserEmail)) {
            when(userRepository.findByEmail(currentUserEmail)).thenReturn(Optional.of(currentUser));

            CheckInCounterResponse response = passengerService.assignCounter(request);

            assertNotNull(response);
            assertNull(response.getAssignedAgentId());
            verify(auditLogService).log(eq(currentUserId), eq("ASSIGNED_CHECK_IN_COUNTER"), eq("CheckInCounter"));
        }
    }

    @Test
    void assignCounter_ThrowsConflictException_WhenCounterInUse() {
        CheckInCounterRequest request = new CheckInCounterRequest();
        request.setCounterNumber("C3");
        request.setFlightId(flightId);

        when(counterRepository.existsByCounterNumberAndStatusNot("C3", CounterStatus.Closed)).thenReturn(true);

        assertThrows(ConflictException.class, () -> passengerService.assignCounter(request));
        verify(counterRepository, never()).save(any());
    }


}
