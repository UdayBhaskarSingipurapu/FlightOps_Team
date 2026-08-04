package com.project.flightOps.service;

import com.project.flightOps.entity.EquipmentAllocation;
import com.project.flightOps.entity.EquipmentMaintenance;
import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.GroundEquipment;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.AllocationStatus;
import com.project.flightOps.enums.EquipmentStatus;
import com.project.flightOps.enums.EquipmentType;
import com.project.flightOps.enums.MaintenanceStatus;
import com.project.flightOps.enums.NotificationCategory;
import com.project.flightOps.enums.Role;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.EquipmentAllocationRepository;
import com.project.flightOps.repository.EquipmentMaintenanceRepository;
import com.project.flightOps.repository.GroundEquipmentRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.EquipmentAllocationRequest;
import com.project.flightOps.requestdto.EquipmentMaintenanceRequest;
import com.project.flightOps.requestdto.EquipmentStatusRequest;
import com.project.flightOps.requestdto.GroundEquipmentRequest;
import com.project.flightOps.responsedto.EquipmentAllocationResponse;
import com.project.flightOps.responsedto.EquipmentMaintenanceResponse;
import com.project.flightOps.responsedto.GroundEquipmentResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
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
class GseServiceTest {

    @Mock
    private GroundEquipmentRepository equipmentRepository;
    @Mock
    private EquipmentAllocationRepository allocationRepository;
    @Mock
    private EquipmentMaintenanceRepository maintenanceRepository;
    @Mock
    private FlightService flightService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private GseService gseService;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    private GroundEquipment equipment;
    private Flight flight;
    private User allocatingUser;
    private User currentUser;
    private User supervisor;

    private final String equipmentId = "EQ-1";
    private final String flightId = "FL-1";
    private final String allocationId = "ALLOC-1";
    private final String maintenanceId = "MAINT-1";
    private final String userEmail = "coordinator@airport.com";
    private final String userId = "USR-1";
    private final String currentUserEmail = "current@airport.com";
    private final String currentUserId = "USR-CURRENT";

    @BeforeEach
    void setUp() {
        equipment = new GroundEquipment();
        equipment.setEquipmentId(equipmentId);
        equipment.setType(EquipmentType.GPU);
        equipment.setRegistrationNumber("REG-100");
        equipment.setCurrentLocation("Gate 1");
        equipment.setStatus(EquipmentStatus.Available);

        flight = new Flight();
        flight.setFlightId(flightId);
        flight.setFlightNumber("AA101");

        allocatingUser = new User();
        allocatingUser.setUserId(userId);
        allocatingUser.setEmail(userEmail);
        allocatingUser.setName("Coordinator One");

        currentUser = new User();
        currentUser.setUserId(currentUserId);
        currentUser.setEmail(currentUserEmail);
        currentUser.setName("Current User");

        supervisor = new User();
        supervisor.setUserId("USR-SUP");
        supervisor.setEmail("sup@airport.com");
        supervisor.setName("Ground Supervisor");
    }

    @AfterEach
    void tearDown() {
        if (securityContextHolderMock != null) {
            securityContextHolderMock.close();
            securityContextHolderMock = null;
        }
    }

    private void mockCurrentAuthenticatedUser() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(currentUserEmail);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

        when(userRepository.findByEmail(currentUserEmail)).thenReturn(Optional.of(currentUser));
    }

    // --- registerEquipment Tests ---

    @Test
    void registerEquipment_Success() {
        GroundEquipmentRequest request = new GroundEquipmentRequest();
        request.setType(EquipmentType.GPU);
        request.setRegistrationNumber("REG-100");
        request.setCurrentLocation("Gate 1");

        when(equipmentRepository.existsByRegistrationNumber("REG-100")).thenReturn(false);
        when(equipmentRepository.save(any(GroundEquipment.class))).thenReturn(equipment);

        GroundEquipmentResponse response = gseService.registerEquipment(request);

        assertNotNull(response);
        assertEquals(equipmentId, response.getEquipmentId());
        assertEquals(EquipmentStatus.Available, response.getStatus());
        verify(equipmentRepository).save(any(GroundEquipment.class));
    }

    @Test
    void registerEquipment_ThrowsConflictException_WhenRegistrationNumberExists() {
        GroundEquipmentRequest request = new GroundEquipmentRequest();
        request.setType(EquipmentType.GPU);
        request.setRegistrationNumber("REG-100");

        when(equipmentRepository.existsByRegistrationNumber("REG-100")).thenReturn(true);

        assertThrows(ConflictException.class, () -> gseService.registerEquipment(request));
        verify(equipmentRepository, never()).save(any());
    }

    // --- getAllEquipment Tests ---

    @Test
    void getAllEquipment_Success() {
        when(equipmentRepository.findAll()).thenReturn(List.of(equipment));

        List<GroundEquipmentResponse> result = gseService.getAllEquipment();

        assertEquals(1, result.size());
        assertEquals(equipmentId, result.get(0).getEquipmentId());
    }

}
