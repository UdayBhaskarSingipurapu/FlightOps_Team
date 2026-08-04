package com.project.flightOps.service;

import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.FlightStatus;
import com.project.flightOps.enums.NotificationCategory;
import com.project.flightOps.enums.Role;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.FlightRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.FlightRequest;
import com.project.flightOps.requestdto.FlightStatusRequest;
import com.project.flightOps.responsedto.FlightResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FlightService flightService;

    private FlightRequest flightRequest;
    private Flight flight;
    private String flightId = "FL123";

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        flightRequest = new FlightRequest();
        flightRequest.setAirlineCode("AA");
        flightRequest.setFlightNumber("AA101");
        flightRequest.setOrigin("JFK");
        flightRequest.setDestination("LAX");
        // Scheduled departure is AFTER arrival per your business logic requirement
        flightRequest.setScheduledArrival(now.plusHours(2));
        flightRequest.setScheduledDeparture(now.plusHours(5));
        flightRequest.setAircraftType("B738");
        flightRequest.setPaxCapacity(180);
        flightRequest.setStand("A1");

        flight = new Flight();
        flight.setFlightId(flightId);
        flight.setAirlineCode("AA");
        flight.setFlightNumber("AA101");
        flight.setOrigin("JFK");
        flight.setDestination("LAX");
        flight.setScheduledArrival(now.plusHours(2));
        flight.setScheduledDeparture(now.plusHours(5));
        flight.setStatus(FlightStatus.Scheduled);
        flight.setStand("A1");
    }

    // --- Create Method Tests ---

    @Test
    void create_Success() {
        when(flightRepository.existsByFlightNumberAndScheduledArrivalBetween(any(), any(), any())).thenReturn(false);
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);

        User user = new User();
        user.setUserId("U1");
        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(user));

        FlightResponse response = flightService.create(flightRequest);

        assertNotNull(response);
        assertEquals(flightId, response.getFlightId());
        verify(notificationService, times(1)).sendNotification(eq("U1"), anyString(), eq(NotificationCategory.FlightSchedule));
        verify(flightRepository).save(any(Flight.class));
    }

    @Test
    void create_ThrowsBadRequestException_WhenDepartureBeforeArrival() {
        // Swap times to trigger failure condition
        flightRequest.setScheduledDeparture(LocalDateTime.now().plusHours(1));
        flightRequest.setScheduledArrival(LocalDateTime.now().plusHours(3));

        assertThrows(BadRequestException.class, () -> flightService.create(flightRequest));
        verify(flightRepository, never()).save(any());
    }

    @Test
    void create_ThrowsConflictException_WhenDuplicateFlightExists() {
        when(flightRepository.existsByFlightNumberAndScheduledArrivalBetween(any(), any(), any())).thenReturn(true);

        assertThrows(ConflictException.class, () -> flightService.create(flightRequest));
        verify(flightRepository, never()).save(any());
    }



}