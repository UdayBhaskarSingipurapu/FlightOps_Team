package com.project.flightOps.service;

import com.project.flightOps.entity.*;
import com.project.flightOps.enums.FlightStatus;
import com.project.flightOps.enums.NotificationCategory;
import com.project.flightOps.enums.RequestStatus;
import com.project.flightOps.enums.Role;
import com.project.flightOps.exception.BadRequestException;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ForbiddenException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.*;
import com.project.flightOps.requestdto.FlightRequest;
import com.project.flightOps.requestdto.FlightStatusRequest;
import com.project.flightOps.responsedto.FlightResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightService {

    private static final String ENTITY_TYPE = "Flight";

    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final HandlingRequestRepository handlingRequestRepository;
    private final TurnaroundPlanRepository turnaroundPlanRepository;
    private final TurnaroundMilestoneRepository milestoneRepository;
    private final AuditLogService auditLogService; // Injected AuditLogService

    @Transactional
    public FlightResponse create(FlightRequest request) {
        log.info("Attempting to create a new flight: {}", request.getFlightNumber());

        // 1. Validation: Origin and Destination cannot be the same (case-insensitive)
        if (request.getOrigin().trim().equalsIgnoreCase(request.getDestination().trim())) {
            log.warn("Flight creation failed: Origin and destination are the same ({})", request.getOrigin());
            throw new BadRequestException("Origin and destination cannot be the same airport");
        }

        // 2. Validation: Departure must be chronologically after arrival (and not at the exact same time)
        if (!request.getScheduledArrival().isBefore(request.getScheduledDeparture())) {
            log.warn("Flight creation failed: Arrival time {} must be before departure time {}",
                    request.getScheduledArrival(), request.getScheduledDeparture());
            throw new BadRequestException("Scheduled arrival must be strictly before the scheduled departure time");
        }

        // 3. Validation: Current logged-in Admin's airport assignment check
        User currentAdmin = getCurrentUser();

        String adminAirportId = currentAdmin.getAirportId();
        if (adminAirportId == null || adminAirportId.trim().isEmpty()) {
            throw new ForbiddenException("You are not assigned to any airport base and cannot manage flights");
        }

        if (!adminAirportId.equalsIgnoreCase(request.getAirlineCode())) {
            log.info("Admin airport code is different from flight airline code. Admin: {}, Flight: {}",
                    adminAirportId, request.getAirlineCode());
            throw new ForbiddenException("Flight airline code mismatch, You can only schedule flights for your airline only");
        }

        boolean matchesOrigin = request.getOrigin().trim().equalsIgnoreCase(adminAirportId.trim());

        if (!matchesOrigin) {
            log.warn("Flight creation failed: Flight origin ({}) does not match admin's airport ({})",
                    request.getOrigin(), adminAirportId);
            throw new BadRequestException("You can only schedule flights departing from your assigned airport (" + adminAirportId + ")");
        }

        // Prevent duplicate flight on same day
        boolean duplicate = flightRepository.existsByFlightNumberAndScheduledArrivalBetween(
                request.getFlightNumber(),
                request.getScheduledArrival().toLocalDate().atStartOfDay(),
                request.getScheduledArrival().toLocalDate().atTime(23, 59, 59)
        );
        if (duplicate) {
            log.warn("Flight creation failed: Duplicate flight number {} on date {}",
                    request.getFlightNumber(), request.getScheduledArrival().toLocalDate());
            throw new ConflictException("Flight " + request.getFlightNumber() + " already scheduled for this date");
        }

        Flight flight = new Flight();
        flight.setAirlineCode(request.getAirlineCode());
        flight.setFlightNumber(request.getFlightNumber());
        flight.setOrigin(request.getOrigin());
        flight.setDestination(request.getDestination());
        flight.setScheduledArrival(request.getScheduledArrival());
        flight.setScheduledDeparture(request.getScheduledDeparture());
        flight.setAircraftType(request.getAircraftType());
        flight.setPaxCapacity(request.getPaxCapacity());
        flight.setStand(request.getStand());
        flight.setStatus(FlightStatus.Scheduled);

        Flight saved = flightRepository.save(flight);
        log.info("Successfully created flight {} with ID: {}", saved.getFlightNumber(), saved.getFlightId());

        // Audit Logging
        auditLogService.log(currentAdmin.getUserId(), "CREATED_FLIGHT", ENTITY_TYPE);

        // Notify all coordinators and supervisors about new flight
        notifyRoles("New flight scheduled: " + saved.getFlightNumber()
                        + " arriving at " + saved.getScheduledArrival(),
                NotificationCategory.FlightSchedule,
                List.of(Role.AirlineCoordinator, Role.GroundSupervisor, Role.PassengerAgent, Role.GSEManager, Role.RampOfficer));

        return toResponse(saved);
    }

    public List<FlightResponse> getToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        log.debug("Fetching today's flights between {} and {}", startOfDay, endOfDay);

        return flightRepository
                .findByScheduledArrivalBetweenOrderByScheduledArrivalAsc(startOfDay, endOfDay)
                .stream().map(this::toResponse).toList();
    }

    public List<FlightResponse> getByAirline(String airlineCode) {
        log.debug("Fetching flights for airline code: {}", airlineCode);
        return flightRepository.findByAirlineCodeIgnoreCaseOrderByScheduledArrivalAsc(airlineCode)
                .stream().map(this::toResponse).toList();
    }

    public FlightResponse getById(String flightId) {
        log.debug("Fetching flight details for ID: {}", flightId);
        return toResponse(findById(flightId));
    }

    @Transactional
    public FlightResponse update(String flightId, FlightRequest request) {
        log.info("Attempting to update flight ID: {}", flightId);

        User currentUser = getCurrentUser();
        Flight flight = findById(flightId);

        if (request.getScheduledDeparture().isBefore(request.getScheduledArrival())) {
            log.warn("Flight update failed for ID {}: Departure time {} is before arrival time {}",
                    flightId, request.getScheduledDeparture(), request.getScheduledArrival());
            throw new BadRequestException("Scheduled departure must be after arrival");
        }

        flight.setAirlineCode(request.getAirlineCode());
        flight.setFlightNumber(request.getFlightNumber());
        flight.setOrigin(request.getOrigin());
        flight.setDestination(request.getDestination());
        flight.setScheduledArrival(request.getScheduledArrival());
        flight.setScheduledDeparture(request.getScheduledDeparture());
        flight.setAircraftType(request.getAircraftType());
        flight.setPaxCapacity(request.getPaxCapacity());
        flight.setStand(request.getStand());

        Flight updated = flightRepository.save(flight);
        log.info("Successfully updated flight ID: {}", flightId);

        // Audit Logging
        auditLogService.log(currentUser.getUserId(), "UPDATED_FLIGHT", ENTITY_TYPE);

        return toResponse(updated);
    }

    @Transactional
    public FlightResponse updateStatus(String flightId, FlightStatusRequest request) {
        User currentUser = getCurrentUser();
        Flight flight = findById(flightId);
        FlightStatus oldStatus = flight.getStatus();
        FlightStatus newStatus = request.getStatus();

        log.info("Attempting to update status for flight ID {} from {} to {}", flightId, oldStatus, newStatus);

        if (!oldStatus.isValidTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                    String.format("Invalid status transition: Cannot change flight status from %s to %s", oldStatus, newStatus)
            );
        }
        if(newStatus.equals(FlightStatus.Arrived)){
            flight.setScheduledArrival(LocalDateTime.now());
            updateMilestonesPlannedTimeWhenFlightArrived(flight);
        }

        flight.setStatus(newStatus);
        Flight saved = flightRepository.save(flight);

        // Audit Logging
        String action = newStatus == FlightStatus.Departed ? "COMPLETED_FLIGHT" : "UPDATED_FLIGHT_STATUS";
        auditLogService.log(currentUser.getUserId(), action, ENTITY_TYPE);

        // Notify on status changes that affect ground ops
        if (newStatus == FlightStatus.Arrived) {
            notifyRoles("Flight " + saved.getFlightNumber() + " has arrived at stand " + saved.getStand(),
                    NotificationCategory.FlightSchedule,
                    List.of(Role.GroundSupervisor, Role.RampOfficer, Role.GSEManager, Role.PassengerAgent));
        } else if (newStatus == FlightStatus.Delayed) {
            notifyRoles("Flight " + saved.getFlightNumber() + " is delayed",
                    NotificationCategory.FlightSchedule,
                    List.of(Role.AirlineCoordinator, Role.GroundSupervisor, Role.RampOfficer));
        }

        return toResponse(saved);
    }


    public List<FlightResponse> getAllFlightsWithHandlingRequestServiceType(String serviceType){
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        log.debug("Fetching today's flights between {} and {}", startOfDay, endOfDay);

        List<Flight> todayFlights = flightRepository.findByScheduledArrivalBetweenOrderByScheduledArrivalAsc(startOfDay, endOfDay);

        return todayFlights.stream()
                .filter(f -> {
                    HandlingRequest hr = handlingRequestRepository.findByFlight_FlightId(f.getFlightId());
                    if (hr == null || hr.getServiceTypes() == null || !hr.getStatus().equals(RequestStatus.Confirmed)) {
                        return false;
                    }
                    return Arrays.stream(hr.getServiceTypes().split(","))
                            .map(String::trim)
                            .anyMatch(serviceType::equalsIgnoreCase);
                })
                .map(this::toResponse)
                .toList();
    }


    private User getCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user profile not found"));
    }

    private void notifyRoles(String message, NotificationCategory category, List<Role> roles) {
        log.info("Sending notifications to roles {} for category {}", roles, category);
        userRepository.findByRoleIn(roles).forEach(user -> {
            log.trace("Dispatching notification to user ID: {}", user.getUserId());
            notificationService.sendNotification(user.getUserId(), message, category);
        });
    }

    public Flight findById(String flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> {
                    log.error("ResourceNotFoundException: Flight not found with ID: {}", flightId);
                    return new ResourceNotFoundException("Flight not found: " + flightId);
                });
    }

    public FlightResponse toResponse(Flight f) {
        return FlightResponse.builder()
                .flightId(f.getFlightId())
                .airlineCode(f.getAirlineCode())
                .flightNumber(f.getFlightNumber())
                .origin(f.getOrigin())
                .destination(f.getDestination())
                .scheduledArrival(f.getScheduledArrival())
                .scheduledDeparture(f.getScheduledDeparture())
                .aircraftType(f.getAircraftType())
                .paxCapacity(f.getPaxCapacity())
                .stand(f.getStand())
                .status(f.getStatus())
                .build();
    }

    public void updateMilestonesPlannedTimeWhenFlightArrived(Flight flight){
        Optional<TurnaroundPlan> plan = turnaroundPlanRepository.findByFlight_FlightId(flight.getFlightId());
        if(plan == null) return;

        List<TurnaroundMilestone> milestones = milestoneRepository.findByTurnaroundPlan_PlanIdOrderByPlannedTimeAsc(plan.get().getPlanId());
        milestones = milestones.stream()
                .map(milestone -> {
                    milestone.setPlannedTime(flight.getScheduledArrival());
                    return milestone;
                }).toList();
        milestoneRepository.saveAll(milestones);
    }
}