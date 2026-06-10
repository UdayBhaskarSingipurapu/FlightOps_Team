package com.project.flightOps.service;

import com.project.flightOps.entity.*;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final CheckInCounterRepository counterRepository;
    private final BoardingGateRepository gateRepository;
    private final SpecialAssistanceRepository assistanceRepository;
    private final FlightService flightService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ─── Check-in Counters ──────────────────────────────────────────────────────

    @Transactional
    public CheckInCounterResponse assignCounter(CheckInCounterRequest request) {
        // Prevent double-assigning the same counter while it's Open/Standby
        if (counterRepository.existsByCounterNumberAndStatusNot(
                request.getCounterNumber(), CounterStatus.Closed)) {
            throw new ConflictException("Counter " + request.getCounterNumber()
                    + " is already in use for another flight");
        }

        Flight flight = flightService.findById(request.getFlightId());

        CheckInCounter counter = new CheckInCounter();
        counter.setCounterNumber(request.getCounterNumber());
        counter.setTerminal(request.getTerminal());
        counter.setFlight(flight);
        counter.setOpenTime(request.getOpenTime());
        counter.setCloseTime(request.getCloseTime());
        counter.setStatus(CounterStatus.Standby);

        if (request.getAssignedAgentId() != null) {
            User agent = findAgent(request.getAssignedAgentId());
            counter.setAssignedAgent(agent);
        }

        CheckInCounter saved = counterRepository.save(counter);

        // Notify airline coordinator
        userRepository.findByRole(Role.AirlineCoordinator).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "Check-in counter " + saved.getCounterNumber()
                                + " assigned to flight " + flight.getFlightNumber(),
                        NotificationCategory.Passenger));

        return toCounterResponse(saved);
    }

    public List<CheckInCounterResponse> getAllCounters() {
        return counterRepository.findAllByOrderByOpenTimeAsc()
                .stream().map(this::toCounterResponse).toList();
    }

    public List<CheckInCounterResponse> getCountersByFlight(String flightId) {
        return counterRepository.findByFlight_FlightId(flightId)
                .stream().map(this::toCounterResponse).toList();
    }

    @Transactional
    public CheckInCounterResponse updateCounterStatus(String counterId, CounterStatusRequest request) {
        CheckInCounter counter = findCounterById(counterId);
        counter.setStatus(request.getStatus());
        CheckInCounter saved = counterRepository.save(counter);

        if (request.getStatus() == CounterStatus.Open) {
            notificationService.sendNotification(
                    saved.getFlight().getAirlineCode(), // notify by airline code convention
                    "Check-in counter " + saved.getCounterNumber()
                            + " is now OPEN for flight " + saved.getFlight().getFlightNumber(),
                    NotificationCategory.Passenger);
        }
        return toCounterResponse(saved);
    }

    // ─── Boarding Gates ──────────────────────────────────────────────────────────

    @Transactional
    public BoardingGateResponse assignGate(BoardingGateRequest request) {
        if (gateRepository.existsByGateNumberAndStatusNot(
                request.getGateNumber(), GateStatus.Closed)) {
            throw new ConflictException("Gate " + request.getGateNumber()
                    + " is already in use for another flight");
        }

        Flight flight = flightService.findById(request.getFlightId());

        BoardingGate gate = new BoardingGate();
        gate.setGateNumber(request.getGateNumber());
        gate.setTerminal(request.getTerminal());
        gate.setFlight(flight);
        gate.setOpenTime(request.getOpenTime());
        gate.setCloseTime(request.getCloseTime());
        gate.setStatus(GateStatus.Closed);

        if (request.getAssignedAgentId() != null) {
            gate.setAssignedAgent(findAgent(request.getAssignedAgentId()));
        }

        BoardingGate saved = gateRepository.save(gate);

        // Notify ground supervisor and ramp officers about gate assignment
        List.of(Role.GroundSupervisor, Role.RampOfficer).forEach(role ->
                userRepository.findByRole(role).forEach(u ->
                        notificationService.sendNotification(u.getUserId(),
                                "Gate " + saved.getGateNumber()
                                        + " assigned to flight " + flight.getFlightNumber(),
                                NotificationCategory.Passenger)));

        return toGateResponse(saved);
    }

    public List<BoardingGateResponse> getAllGates() {
        return gateRepository.findAllByOrderByOpenTimeAsc()
                .stream().map(this::toGateResponse).toList();
    }

    public List<BoardingGateResponse> getGatesByFlight(String flightId) {
        return gateRepository.findByFlight_FlightId(flightId)
                .stream().map(this::toGateResponse).toList();
    }

    @Transactional
    public BoardingGateResponse updateGateStatus(String gateId, GateStatusRequest request) {
        BoardingGate gate = findGateById(gateId);
        gate.setStatus(request.getStatus());
        BoardingGate saved = gateRepository.save(gate);

        // Notify ramp officers when boarding starts
        if (request.getStatus() == GateStatus.Boarding) {
            userRepository.findByRole(Role.RampOfficer).forEach(u ->
                    notificationService.sendNotification(u.getUserId(),
                            "Boarding started at gate " + saved.getGateNumber()
                                    + " for flight " + saved.getFlight().getFlightNumber(),
                            NotificationCategory.Passenger));
        }
        return toGateResponse(saved);
    }

    // ─── Special Assistance ──────────────────────────────────────────────────────

    @Transactional
    public SpecialAssistanceResponse createAssistanceRequest(SpecialAssistanceRequest request) {
        Flight flight = flightService.findById(request.getFlightId());

        SpecialAssistance assistance = new SpecialAssistance();
        assistance.setFlight(flight);
        assistance.setPassengerName(request.getPassengerName());
        assistance.setAssistanceType(request.getAssistanceType());
        assistance.setStatus(AssistanceStatus.Requested);

        SpecialAssistance saved = assistanceRepository.save(assistance);

        // Notify all passenger agents about new request
        userRepository.findByRole(Role.PassengerAgent).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "Special assistance requested: " + request.getAssistanceType()
                                + " for " + request.getPassengerName()
                                + " on flight " + flight.getFlightNumber(),
                        NotificationCategory.Passenger));

        return toAssistanceResponse(saved);
    }

    public List<SpecialAssistanceResponse> getAllAssistanceRequests() {
        return assistanceRepository.findAllByOrderByStatusAsc()
                .stream().map(this::toAssistanceResponse).toList();
    }

    @Transactional
    public SpecialAssistanceResponse assignAgent(String assistanceId, AssistanceAssignRequest request) {
        SpecialAssistance assistance = findAssistanceById(assistanceId);

        if (assistance.getStatus() == AssistanceStatus.Completed) {
            throw new BadRequestException("This assistance request is already completed");
        }

        User agent = findAgent(request.getAgentId());
        assistance.setAssignedAgent(agent);
        assistance.setStatus(AssistanceStatus.Assigned);

        SpecialAssistance saved = assistanceRepository.save(assistance);

        // Notify the assigned agent
        notificationService.sendNotification(agent.getUserId(),
                "You have been assigned to assist passenger " + saved.getPassengerName()
                        + " (" + saved.getAssistanceType() + ") on flight "
                        + saved.getFlight().getFlightNumber(),
                NotificationCategory.Passenger);

        return toAssistanceResponse(saved);
    }

    @Transactional
    public SpecialAssistanceResponse completeAssistance(String assistanceId) {
        SpecialAssistance assistance = findAssistanceById(assistanceId);

        if (assistance.getStatus() != AssistanceStatus.Assigned) {
            throw new BadRequestException(
                    "Assistance must be Assigned before it can be marked complete");
        }
        assistance.setStatus(AssistanceStatus.Completed);
        return toAssistanceResponse(assistanceRepository.save(assistance));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private User findAgent(String agentId) {
        return userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + agentId));
    }

    private CheckInCounter findCounterById(String id) {
        return counterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found: " + id));
    }

    private BoardingGate findGateById(String id) {
        return gateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gate not found: " + id));
    }

    private SpecialAssistance findAssistanceById(String id) {
        return assistanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assistance request not found: " + id));
    }

    private CheckInCounterResponse toCounterResponse(CheckInCounter c) {
        return CheckInCounterResponse.builder()
                .counterId(c.getCounterId())
                .counterNumber(c.getCounterNumber())
                .terminal(c.getTerminal())
                .flightId(c.getFlight().getFlightId())
                .flightNumber(c.getFlight().getFlightNumber())
                .assignedAgentId(c.getAssignedAgent() != null
                        ? c.getAssignedAgent().getUserId() : null)
                .assignedAgentName(c.getAssignedAgent() != null
                        ? c.getAssignedAgent().getName() : null)
                .openTime(c.getOpenTime())
                .closeTime(c.getCloseTime())
                .status(c.getStatus())
                .build();
    }

    private BoardingGateResponse toGateResponse(BoardingGate g) {
        return BoardingGateResponse.builder()
                .gateId(g.getGateId())
                .gateNumber(g.getGateNumber())
                .terminal(g.getTerminal())
                .flightId(g.getFlight().getFlightId())
                .flightNumber(g.getFlight().getFlightNumber())
                .assignedAgentId(g.getAssignedAgent() != null
                        ? g.getAssignedAgent().getUserId() : null)
                .assignedAgentName(g.getAssignedAgent() != null
                        ? g.getAssignedAgent().getName() : null)
                .openTime(g.getOpenTime())
                .closeTime(g.getCloseTime())
                .status(g.getStatus())
                .build();
    }

    private SpecialAssistanceResponse toAssistanceResponse(SpecialAssistance a) {
        return SpecialAssistanceResponse.builder()
                .assistanceId(a.getAssistanceId())
                .flightId(a.getFlight().getFlightId())
                .flightNumber(a.getFlight().getFlightNumber())
                .passengerName(a.getPassengerName())
                .assistanceType(a.getAssistanceType())
                .assignedAgentId(a.getAssignedAgent() != null
                        ? a.getAssignedAgent().getUserId() : null)
                .assignedAgentName(a.getAssignedAgent() != null
                        ? a.getAssignedAgent().getName() : null)
                .status(a.getStatus())
                .build();
    }
}
