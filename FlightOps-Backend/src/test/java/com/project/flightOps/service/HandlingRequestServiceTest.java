package com.project.flightOps.service;

import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.HandlingRequest;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.NotificationCategory;
import com.project.flightOps.enums.RequestStatus;
import com.project.flightOps.enums.Role;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.HandlingRequestRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.HandlingRequestDto;
import com.project.flightOps.requestdto.HandlingStatusRequest;
import com.project.flightOps.responsedto.HandlingRequestResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandlingRequestServiceTest {

    @Mock
    private HandlingRequestRepository handlingRequestRepository;

    @Mock
    private FlightService flightService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private HandlingRequestService handlingRequestService;

    private Flight mockFlight;
    private User mockRequester;
    private User mockSupervisor;
    private HandlingRequest mockRequest;
    private HandlingRequestDto mockDto;

    private final String flightId = "FL-100";
    private final String requestId = "REQ-1";
    private final String requesterEmail = "coordinator@airport.com";
    private final String requesterId = "USR-COORD";
    private final String supervisorId = "USR-SUP";

    @BeforeEach
    void setUp() {
        mockFlight = new Flight();
        mockFlight.setFlightId(flightId);
        mockFlight.setFlightNumber("AA202");

        mockRequester = new User();
        mockRequester.setUserId(requesterId);
        mockRequester.setEmail(requesterEmail);
        mockRequester.setName("Coordinator Bob");
        mockRequester.setRole(Role.AirlineCoordinator);

        mockSupervisor = new User();
        mockSupervisor.setUserId(supervisorId);
        mockSupervisor.setEmail("supervisor@airport.com");
        mockSupervisor.setName("Supervisor Sam");
        mockSupervisor.setRole(Role.GroundSupervisor);

        mockRequest = new HandlingRequest();
        mockRequest.setRequestId(requestId);
        mockRequest.setFlight(mockFlight);
        mockRequest.setAirlineId("AA");
        mockRequest.setServiceTypes("Ramp,Baggage");
        mockRequest.setSpecialRequirements("None");
        mockRequest.setRequestedBy(mockRequester);
        mockRequest.setStatus(RequestStatus.Received);

        mockDto = new HandlingRequestDto();
        mockDto.setFlightId(flightId);
        mockDto.setAirlineId("AA");
        mockDto.setServiceTypes("Ramp,Baggage");
        mockDto.setSpecialRequirements("None");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String username) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(username, null);
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // --- create Tests ---

    @Test
    void create_Success() {
        when(flightService.findById(flightId)).thenReturn(mockFlight);
        when(handlingRequestRepository.existsByFlightAndStatusNot(mockFlight, RequestStatus.Disputed))
                .thenReturn(false);
        when(userRepository.findByEmail(requesterEmail)).thenReturn(Optional.of(mockRequester));
        when(handlingRequestRepository.save(any(HandlingRequest.class))).thenReturn(mockRequest);
        when(userRepository.findByRole(Role.GroundSupervisor)).thenReturn(List.of(mockSupervisor));

        HandlingRequestResponse response = handlingRequestService.create(mockDto, requesterEmail);

        assertNotNull(response);
        assertEquals(requestId, response.getRequestId());
        assertEquals(flightId, response.getFlightId());
        assertEquals(RequestStatus.Received, response.getStatus());
        verify(handlingRequestRepository).save(any(HandlingRequest.class));
        verify(auditLogService).log(eq(requesterId), eq("CREATED_HANDLING_REQUEST"), eq("HandlingRequest"));
        verify(notificationService).sendNotification(eq(supervisorId), anyString(), eq(NotificationCategory.FlightSchedule));
    }

    @Test
    void create_ThrowsConflictException_WhenDuplicateActiveRequestExists() {
        when(flightService.findById(flightId)).thenReturn(mockFlight);
        when(handlingRequestRepository.existsByFlightAndStatusNot(mockFlight, RequestStatus.Disputed))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> handlingRequestService.create(mockDto, requesterEmail));
        verify(handlingRequestRepository, never()).save(any());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void create_ThrowsResourceNotFoundException_WhenRequesterNotFound() {
        when(flightService.findById(flightId)).thenReturn(mockFlight);
        when(handlingRequestRepository.existsByFlightAndStatusNot(mockFlight, RequestStatus.Disputed))
                .thenReturn(false);
        when(userRepository.findByEmail(requesterEmail)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> handlingRequestService.create(mockDto, requesterEmail));
        verify(handlingRequestRepository, never()).save(any());
    }


}
