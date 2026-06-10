package com.project.flightOps.service;

import com.project.flightOps.entity.Flight;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public FlightResponse create(FlightRequest request) {
        if (request.getScheduledDeparture().isBefore(request.getScheduledArrival())) {
            throw new BadRequestException("Scheduled departure must be after arrival");
        }
        // Prevent duplicate flight on same day
        boolean duplicate = flightRepository.existsByFlightNumberAndScheduledArrivalBetween(
                request.getFlightNumber(),
                request.getScheduledArrival().toLocalDate().atStartOfDay(),
                request.getScheduledArrival().toLocalDate().atTime(23, 59, 59)
        );
        if (duplicate) {
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

        // Notify all coordinators and supervisors about new flight
        notifyRoles("New flight scheduled: " + saved.getFlightNumber()
                        + " arriving at " + saved.getScheduledArrival(),
                NotificationCategory.FlightSchedule,
                List.of(Role.AirlineCoordinator, Role.GroundSupervisor));

        return toResponse(saved);
    }

    public List<FlightResponse> getToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        return flightRepository
                .findByScheduledArrivalBetweenOrderByScheduledArrivalAsc(startOfDay, endOfDay)
                .stream().map(this::toResponse).toList();
    }

    public List<FlightResponse> getByAirline(String airlineCode) {
        return flightRepository.findByAirlineCodeOrderByScheduledArrivalAsc(airlineCode)
                .stream().map(this::toResponse).toList();
    }

    public FlightResponse getById(String flightId) {
        return toResponse(findById(flightId));
    }

    @Transactional
    public FlightResponse update(String flightId, FlightRequest request) {
        Flight flight = findById(flightId);
        if (request.getScheduledDeparture().isBefore(request.getScheduledArrival())) {
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
        return toResponse(flightRepository.save(flight));
    }

    @Transactional
    public FlightResponse updateStatus(String flightId, FlightStatusRequest request) {
        Flight flight = findById(flightId);
        FlightStatus oldStatus = flight.getStatus();
        flight.setStatus(request.getStatus());
        Flight saved = flightRepository.save(flight);

        // Notify on status changes that affect ground ops
        if (request.getStatus() == FlightStatus.Arrived) {
            notifyRoles("Flight " + saved.getFlightNumber() + " has arrived at stand " + saved.getStand(),
                    NotificationCategory.FlightSchedule,
                    List.of(Role.GroundSupervisor, Role.RampOfficer, Role.GSEManager, Role.PassengerAgent));
        } else if (request.getStatus() == FlightStatus.Delayed) {
            notifyRoles("Flight " + saved.getFlightNumber() + " is delayed",
                    NotificationCategory.FlightSchedule,
                    List.of(Role.AirlineCoordinator, Role.GroundSupervisor));
        }

        return toResponse(saved);
    }

    private void notifyRoles(String message, NotificationCategory category, List<Role> roles) {
        userRepository.findByRoleIn(roles).forEach(user ->
                notificationService.sendNotification(user.getUserId(), message, category));
    }

    public Flight findById(String flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found: " + flightId));
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
}
