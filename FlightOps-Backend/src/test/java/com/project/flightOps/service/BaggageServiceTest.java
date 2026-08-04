package com.project.flightOps.service;

import com.project.flightOps.entity.BaggageOperation;
import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.MishandledBaggage;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.Direction;
import com.project.flightOps.enums.MishandledStatus;
import com.project.flightOps.enums.MishandledType;
import com.project.flightOps.enums.NotificationCategory;
import com.project.flightOps.enums.OperationStatus;
import com.project.flightOps.enums.Role;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.BaggageOperationRepository;
import com.project.flightOps.repository.MishandledBaggageRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.BaggageCountRequest;
import com.project.flightOps.requestdto.BaggageOperationRequest;
import com.project.flightOps.requestdto.MishandledBaggageRequest;
import com.project.flightOps.requestdto.MishandledStatusRequest;
import com.project.flightOps.responsedto.BaggageOperationResponse;
import com.project.flightOps.responsedto.MishandledBaggageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaggageServiceTest {

    @Mock
    private BaggageOperationRepository baggageOperationRepository;
    @Mock
    private MishandledBaggageRepository mishandledBaggageRepository;
    @Mock
    private FlightService flightService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BaggageService baggageService;

    private Flight mockFlight;
    private User mockOperator;
    private User mockSupervisor;
    private User mockRampOfficer;
    private BaggageOperation mockOperation;
    private MishandledBaggage mockMishandled;
    private BaggageOperationRequest operationRequest;
    private MishandledBaggageRequest mishandledRequest;

    private final String flightId = "FL-100";
    private final String operatorEmail = "operator@airport.com";
    private final String operatorId = "USR-OP";
    private final String operationId = "OP-001";
    private final String mishandleId = "MB-001";
    private final String bagTagNumber = "TAG-999";

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        mockFlight = new Flight();
        mockFlight.setFlightId(flightId);
        mockFlight.setFlightNumber("BA202");

        mockOperator = new User();
        mockOperator.setUserId(operatorId);
        mockOperator.setEmail(operatorEmail);
        mockOperator.setName("Operator One");

        mockSupervisor = new User();
        mockSupervisor.setUserId("USR-SUP");
        mockSupervisor.setEmail("sup@airport.com");
        mockSupervisor.setName("Supervisor One");

        mockRampOfficer = new User();
        mockRampOfficer.setUserId("USR-RAMP");
        mockRampOfficer.setEmail("ramp@airport.com");
        mockRampOfficer.setName("Ramp One");

        mockOperation = new BaggageOperation();
        mockOperation.setOperationId(operationId);
        mockOperation.setFlight(mockFlight);
        mockOperation.setDirection(Direction.Inbound);
        mockOperation.setTotalBagsExpected(100);
        mockOperation.setTotalBagsProcessed(0);
        mockOperation.setOperator(mockOperator);
        mockOperation.setStartTime(now);
        mockOperation.setStatus(OperationStatus.InProgress);

        mockMishandled = new MishandledBaggage();
        mockMishandled.setMishandleId(mishandleId);
        mockMishandled.setFlight(mockFlight);
        mockMishandled.setPassengerName("John Doe");
        mockMishandled.setBagTagNumber(bagTagNumber);
        mockMishandled.setMishandleType(MishandledType.Lost);
        mockMishandled.setReportedDate(now);
        mockMishandled.setStatus(MishandledStatus.Reported);

        operationRequest = new BaggageOperationRequest();
        operationRequest.setFlightId(flightId);
        operationRequest.setDirection(Direction.Inbound);
        operationRequest.setTotalBagsExpected(100);
        operationRequest.setStartTime(now);

        mishandledRequest = new MishandledBaggageRequest();
        mishandledRequest.setFlightId(flightId);
        mishandledRequest.setPassengerName("John Doe");
        mishandledRequest.setBagTagNumber(bagTagNumber);
        mishandledRequest.setMishandleType(MishandledType.Lost);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(String email, User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    // --- createOperation Tests ---

    @Test
    void createOperation_Success() {
        when(flightService.findById(flightId)).thenReturn(mockFlight);
        when(baggageOperationRepository.existsByFlight_FlightIdAndDirection(flightId, Direction.Inbound))
                .thenReturn(false);
        when(userRepository.findByEmail(operatorEmail)).thenReturn(Optional.of(mockOperator));
        when(baggageOperationRepository.save(any(BaggageOperation.class))).thenReturn(mockOperation);

        BaggageOperationResponse response = baggageService.createOperation(operationRequest, operatorEmail);

        assertNotNull(response);
        assertEquals(operationId, response.getOperationId());
        assertEquals(OperationStatus.InProgress, response.getStatus());
        verify(baggageOperationRepository).save(any(BaggageOperation.class));
        verify(auditLogService).log(eq(operatorId), eq("CREATED_BAGGAGE_OPERATION"), eq("BaggageOperation"));
    }

    @Test
    void createOperation_ThrowsConflictException_WhenDuplicateDirectionExists() {
        when(flightService.findById(flightId)).thenReturn(mockFlight);
        when(baggageOperationRepository.existsByFlight_FlightIdAndDirection(flightId, Direction.Inbound))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> baggageService.createOperation(operationRequest, operatorEmail));
        verify(baggageOperationRepository, never()).save(any());
    }

    @Test
    void createOperation_ThrowsResourceNotFoundException_WhenOperatorMissing() {
        when(flightService.findById(flightId)).thenReturn(mockFlight);
        when(baggageOperationRepository.existsByFlight_FlightIdAndDirection(flightId, Direction.Inbound))
                .thenReturn(false);
        when(userRepository.findByEmail(operatorEmail)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> baggageService.createOperation(operationRequest, operatorEmail));
        verify(baggageOperationRepository, never()).save(any());
    }

}
