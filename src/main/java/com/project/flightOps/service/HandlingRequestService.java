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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HandlingRequestService {

    private static final String ENTITY_TYPE = "HandlingRequest";

    private final HandlingRequestRepository handlingRequestRepository;
    private final FlightService flightService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService; // 1. Injected AuditLogService

    @Transactional
    public HandlingRequestResponse create(HandlingRequestDto dto, String requestedByUserId) {
        log.info("Initiating creation of handling request for flightId: {} by user: {}", dto.getFlightId(), requestedByUserId);

        Flight flight = flightService.findById(dto.getFlightId());

        // Prevent duplicate active request for same flight
        boolean exists = handlingRequestRepository.existsByFlightAndStatusNot(
                flight, RequestStatus.Disputed);
        if (exists) {
            log.warn("Conflict detected: Active handling request already exists for flight number: {}", flight.getFlightNumber());
            throw new ConflictException("A handling request already exists for flight: "
                    + flight.getFlightNumber());
        }

        User requestedBy = userRepository.findByEmail(requestedByUserId)
                .orElseThrow(() -> {
                    log.error("Failed to create handling request. User not found: {}", requestedByUserId);
                    return new ResourceNotFoundException("User not found");
                });

        HandlingRequest request = new HandlingRequest();
        request.setFlight(flight);
        request.setAirlineId(dto.getAirlineId());
        request.setServiceTypes(dto.getServiceTypes());
        request.setSpecialRequirements(dto.getSpecialRequirements());
        request.setRequestedBy(requestedBy);
        request.setStatus(RequestStatus.Received);

        HandlingRequest saved = handlingRequestRepository.save(request);
        log.info("Handling request successfully created with ID: {} for flight: {}", saved.getRequestId(), flight.getFlightNumber());

        // 2. Audit Logging
        auditLogService.log(requestedBy.getUserId(), "CREATED_HANDLING_REQUEST", ENTITY_TYPE);

        // Notify supervisors
        log.debug("Dispatching notifications to Ground Supervisors for new request: {}", saved.getRequestId());
        userRepository.findByRole(Role.GroundSupervisor).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "New handling request received for flight " + flight.getFlightNumber(),
                        NotificationCategory.FlightSchedule));

        return toResponse(saved);
    }

    public List<HandlingRequestResponse> getAll() {
        log.debug("Fetching all handling requests");
        return handlingRequestRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<HandlingRequestResponse> getByAirline(String airlineId) {
        log.debug("Fetching handling requests for airline ID: {}", airlineId);
        return handlingRequestRepository.findByAirlineIdOrderByStatusAsc(airlineId)
                .stream().map(this::toResponse).toList();
    }

    public HandlingRequestResponse getById(String requestId) {
        log.debug("Fetching handling request details for ID: {}", requestId);
        return toResponse(findById(requestId));
    }

    public List<HandlingRequestResponse> getByUserId(String userId) {
        log.debug("Fetching handling request details for ID: {}", userId);
        return handlingRequestRepository.findAllByRequestedByUserId(userId).stream().map(this::toResponse).toList();
    }

    public HandlingRequestResponse getByFlight(String flightId) {
        log.debug("Fetching handling requests for flight ID: {}", flightId);
        HandlingRequest handlingRequest = handlingRequestRepository.findByFlight_FlightId(flightId);
        return toResponse(handlingRequest);

    }

    @Transactional
    public HandlingRequestResponse updateStatus(String requestId, HandlingStatusRequest statusRequest) {
        log.info("Attempting to update status of request ID: {} to {}", requestId, statusRequest.getStatus());

        HandlingRequest request = findById(requestId);

        // Guard: can't re-confirm a completed request
        if (request.getStatus() == RequestStatus.Completed) {
            log.warn("Invalid operation: Attempted to modify completed handling request ID: {}", requestId);
            throw new BadRequestException("Cannot modify a completed handling request");
        }

        RequestStatus oldStatus = request.getStatus();
        request.setStatus(statusRequest.getStatus());
        HandlingRequest saved = handlingRequestRepository.save(request);
        log.info("Successfully updated request ID: {} status from {} to {}", requestId, oldStatus, saved.getStatus());

        // 3. Audit Logging
        User currentUser = getCurrentUser();
        String action = saved.getStatus() == RequestStatus.Completed ? "COMPLETED_HANDLING_REQUEST" : "UPDATED_HANDLING_REQUEST_STATUS";
        auditLogService.log(currentUser.getUserId(), action, ENTITY_TYPE);

        // Notify coordinator about confirmation/rejection
        log.debug("Notifying coordinator (User ID: {}) regarding status update to {}", saved.getRequestedBy().getUserId(), saved.getStatus());
        notificationService.sendNotification(
                saved.getRequestedBy().getUserId(),
                "Your handling request for flight " + saved.getFlight().getFlightNumber()
                        + " is now " + saved.getStatus(),
                NotificationCategory.FlightSchedule);

        return toResponse(saved);
    }

    private User getCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user profile not found"));
    }

    private HandlingRequest findById(String id) {
        return handlingRequestRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Resource lookup failed. Handling request with ID: {} not found", id);
                    return new ResourceNotFoundException("Handling request not found: " + id);
                });
    }

    private HandlingRequestResponse toResponse(HandlingRequest h) {
        return HandlingRequestResponse.builder()
                .requestId(h.getRequestId())
                .flightId(h.getFlight().getFlightId())
                .flightNumber(h.getFlight().getFlightNumber())
                .airlineId(h.getAirlineId())
                .serviceTypes(h.getServiceTypes())
                .specialRequirements(h.getSpecialRequirements())
                .requestedById(h.getRequestedBy().getUserId())
                .requestedByName(h.getRequestedBy().getName())
                .status(h.getStatus())
                .build();
    }
}