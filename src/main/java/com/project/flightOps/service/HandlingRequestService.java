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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HandlingRequestService {

    private final HandlingRequestRepository handlingRequestRepository;
    private final FlightService flightService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public HandlingRequestResponse create(HandlingRequestDto dto, String requestedByUserId) {
        Flight flight = flightService.findById(dto.getFlightId());

        // Prevent duplicate active request for same flight
        boolean exists = handlingRequestRepository.existsByFlightAndStatusNot(
                flight, RequestStatus.Disputed);
        if (exists) {
            throw new ConflictException("A handling request already exists for flight: "
                    + flight.getFlightNumber());
        }

        User requestedBy = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        HandlingRequest request = new HandlingRequest();
        request.setFlight(flight);
        request.setAirlineId(dto.getAirlineId());
        request.setServiceTypes(dto.getServiceTypes());
        request.setSpecialRequirements(dto.getSpecialRequirements());
        request.setRequestedBy(requestedBy);
        request.setStatus(RequestStatus.Received);

        HandlingRequest saved = handlingRequestRepository.save(request);

        // Notify supervisors
        userRepository.findByRole(Role.GroundSupervisor).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "New handling request received for flight " + flight.getFlightNumber(),
                        NotificationCategory.FlightSchedule));

        return toResponse(saved);
    }

    public List<HandlingRequestResponse> getAll() {
        return handlingRequestRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<HandlingRequestResponse> getByAirline(String airlineId) {
        return handlingRequestRepository.findByAirlineIdOrderByStatusAsc(airlineId)
                .stream().map(this::toResponse).toList();
    }

    public HandlingRequestResponse getById(String requestId) {
        return toResponse(findById(requestId));
    }

    public List<HandlingRequestResponse> getByFlight(String flightId) {
        return handlingRequestRepository.findByFlight_FlightId(flightId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public HandlingRequestResponse updateStatus(String requestId, HandlingStatusRequest statusRequest) {
        HandlingRequest request = findById(requestId);

        // Guard: can't re-confirm a completed request
        if (request.getStatus() == RequestStatus.Completed) {
            throw new BadRequestException("Cannot modify a completed handling request");
        }

        request.setStatus(statusRequest.getStatus());
        HandlingRequest saved = handlingRequestRepository.save(request);

        // Notify coordinator about confirmation/rejection
        notificationService.sendNotification(
                saved.getRequestedBy().getUserId(),
                "Your handling request for flight " + saved.getFlight().getFlightNumber()
                        + " is now " + saved.getStatus(),
                NotificationCategory.FlightSchedule);

        return toResponse(saved);
    }

    private HandlingRequest findById(String id) {
        return handlingRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Handling request not found: " + id));
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
