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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassengerService {

    private static final String COUNTER_ENTITY_TYPE = "CheckInCounter";
    private static final String GATE_ENTITY_TYPE = "BoardingGate";
    private static final String ASSISTANCE_ENTITY_TYPE = "SpecialAssistance";

    private final CheckInCounterRepository counterRepository;
    private final BoardingGateRepository gateRepository;
    private final SpecialAssistanceRepository assistanceRepository;
    private final FlightService flightService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService; // Injected AuditLogService

    // ─── Check-in Counters ──────────────────────────────────────────────────────

    @Transactional
    public CheckInCounterResponse assignCounter(CheckInCounterRequest request) {
        log.info("Attempting to assign check-in counter {} for flight ID {}", request.getCounterNumber(), request.getFlightId());

        // Prevent double-assigning the same counter while it's Open/Standby
        if (counterRepository.existsByCounterNumberAndStatusNot(
                request.getCounterNumber(), CounterStatus.Closed)) {
            log.warn("Conflict detected: Counter {} is already in use", request.getCounterNumber());
            throw new ConflictException("Counter " + request.getCounterNumber()
                    + " is already in use for another flight");
        }

        if(request.getCloseTime() != null && request.getCloseTime().equals(request.getOpenTime())){
            log.info("Bad request: Counter open time {} and close time {} is same", request.getOpenTime(), request.getCloseTime());
            throw new BadRequestException("Counter open time cannot be same as close time");
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
            log.debug("Assigning agent ID {} to counter {}", request.getAssignedAgentId(), request.getCounterNumber());
            User agent = findAgent(request.getAssignedAgentId());
            counter.setAssignedAgent(agent);
        }

        CheckInCounter saved = counterRepository.save(counter);
        log.info("Successfully assigned counter ID {} (Number: {}) to flight {}", saved.getCounterId(), saved.getCounterNumber(), flight.getFlightNumber());

        // Audit Logging
        String actorUserId = saved.getAssignedAgent() != null
                ? saved.getAssignedAgent().getUserId()
                : getCurrentUser().getUserId();
        auditLogService.log(actorUserId, "ASSIGNED_CHECK_IN_COUNTER", COUNTER_ENTITY_TYPE);

        // Notify airline coordinator
        userRepository.findByRole(Role.AirlineCoordinator).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "Check-in counter " + saved.getCounterNumber()
                                + " assigned to flight " + flight.getFlightNumber(),
                        NotificationCategory.Passenger));

        return toCounterResponse(saved);
    }

    public List<CheckInCounterResponse> getAllCounters() {
        log.debug("Fetching all check-in counters ordered by open time");
        return counterRepository.findAllByOrderByOpenTimeAsc()
                .stream().map(this::toCounterResponse).toList();
    }

    public List<CheckInCounterResponse> getAllCountersAssignedByAgent(String userId){
        log.debug("fetching all checking counters assigned by agent");
        return counterRepository.findByAssignedAgentUserId(userId)
                .stream().map(this::toCounterResponse).toList();
    }

    public List<CheckInCounterResponse> getCountersByFlight(String flightId) {
        log.debug("Fetching check-in counters for flight ID {}", flightId);
        return counterRepository.findByFlight_FlightId(flightId)
                .stream().map(this::toCounterResponse).toList();
    }

    @Transactional
    public CheckInCounterResponse updateCounterStatus(String counterId, CounterStatusRequest request) {
        log.info("Updating status of counter ID {} to {}", counterId, request.getStatus());
        CheckInCounter counter = findCounterById(counterId);

        CounterStatus oldStatus = counter.getStatus();
        counter.setStatus(request.getStatus());
        CheckInCounter saved = counterRepository.save(counter);
        log.debug("Counter ID {} shifted status from {} to {}", counterId, oldStatus, saved.getStatus());

        // Audit Logging
        User currentUser = getCurrentUser();
        auditLogService.log(currentUser.getUserId(), "UPDATED_CHECK_IN_COUNTER", COUNTER_ENTITY_TYPE);

        if(request.getStatus() == CounterStatus.Open) {
            userRepository.findByRole(Role.AirlineCoordinator).forEach(u ->
                    notificationService.sendNotification(u.getUserId(),
                            "Check-in counter " + saved.getCounterNumber()
                                    + " is now OPEN for flight " + saved.getFlight().getFlightNumber(),
                            NotificationCategory.Passenger));
        }
        return toCounterResponse(saved);
    }

    // ─── Boarding Gates ──────────────────────────────────────────────────────────

    @Transactional
    public BoardingGateResponse assignGate(BoardingGateRequest request) {
        log.info("Attempting to assign gate {} for flight ID {}", request.getGateNumber(), request.getFlightId());

        if (gateRepository.existsByGateNumberAndStatusNot(
                request.getGateNumber(), GateStatus.Closed)) {
            log.warn("Conflict detected: Gate {} is already in use", request.getGateNumber());
            throw new ConflictException("Gate " + request.getGateNumber()
                    + " is already in use for another flight");
        }

        if(request.getCloseTime() != null && request.getCloseTime().equals(request.getOpenTime())){
            log.info("Bad request: Boarding gate open time {} and close time {} is same", request.getOpenTime(), request.getCloseTime());
            throw new BadRequestException("Boarding gate open time cannot be same as close time");
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
            log.debug("Assigning agent ID {} to gate {}", request.getAssignedAgentId(), request.getGateNumber());
            gate.setAssignedAgent(findAgent(request.getAssignedAgentId()));
        }

        BoardingGate saved = gateRepository.save(gate);
        log.info("Successfully assigned gate ID {} (Number: {}) to flight {}", saved.getGateId(), saved.getGateNumber(), flight.getFlightNumber());

        // Audit Logging
        String actorUserId = saved.getAssignedAgent() != null
                ? saved.getAssignedAgent().getUserId()
                : getCurrentUser().getUserId();
        auditLogService.log(actorUserId, "ASSIGNED_BOARDING_GATE", GATE_ENTITY_TYPE);

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
        log.debug("Fetching all boarding gates ordered by open time");
        return gateRepository.findAllByOrderByOpenTimeAsc()
                .stream().map(this::toGateResponse).toList();
    }

    public List<BoardingGateResponse> getAllBoardingGatesAssignedByAgent(String userId){
        log.debug("fetching all boarding gates assigned by agent");
        return gateRepository.findByAssignedAgentUserId(userId)
                .stream().map(this::toGateResponse).toList();
    }

    public List<BoardingGateResponse> getGatesByFlight(String flightId) {
        log.debug("Fetching boarding gates for flight ID {}", flightId);
        return gateRepository.findByFlight_FlightId(flightId)
                .stream().map(this::toGateResponse).toList();
    }

    @Transactional
    public BoardingGateResponse updateGateStatus(String gateId, GateStatusRequest request) {
        log.info("Updating status of gate ID {} to {}", gateId, request.getStatus());
        BoardingGate gate = findGateById(gateId);

        GateStatus oldStatus = gate.getStatus();
        gate.setStatus(request.getStatus());
        BoardingGate saved = gateRepository.save(gate);
        log.debug("Gate ID {} shifted status from {} to {}", gateId, oldStatus, saved.getStatus());

        // Audit Logging
        User currentUser = getCurrentUser();
        auditLogService.log(currentUser.getUserId(), "UPDATED_BOARDING_GATE", GATE_ENTITY_TYPE);

        // Notify ramp officers when boarding starts
        if (request.getStatus() == GateStatus.Boarding) {
            userRepository.findByRole(Role.RampOfficer).forEach(u ->
                    notificationService.sendNotification(u.getUserId(),
                            "Boarding started at gate " + saved.getGateNumber()
                                    + " for flight " + saved.getFlight().getFlightNumber(),
                            NotificationCategory.Passenger));
        }

        if(request.getStatus().equals(GateStatus.Closed)) {
            userRepository.findByRole(Role.GroundSupervisor).forEach(u ->
                    notificationService.sendNotification(u.getUserId(),
                            "Gate " + saved.getGateNumber()
                                    + " is now CLOSED for flight " + saved.getFlight().getFlightNumber(),
                            NotificationCategory.Passenger));
            userRepository.findByRole(Role.RampOfficer).forEach(u ->
                    notificationService.sendNotification(u.getUserId(),
                            "Gate " + saved.getGateNumber()
                                    + " is now CLOSED for flight " + saved.getFlight().getFlightNumber(),
                            NotificationCategory.Passenger));
        }
        return toGateResponse(saved);
    }

    // ─── Special Assistance ──────────────────────────────────────────────────────

    @Transactional
    public SpecialAssistanceResponse createAssistanceRequest(SpecialAssistanceRequest request) {
        String userId = request.getUserId();
        log.info("Creating special assistance request type '{}' for passenger '{}' on flight ID {} by agent ID {}",
                request.getAssistanceType(), request.getPassengerName(), request.getFlightId(), userId);

        Flight flight = flightService.findById(request.getFlightId());
        User agent = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + userId));

        SpecialAssistance assistance = new SpecialAssistance();
        assistance.setFlight(flight);
        assistance.setPassengerName(request.getPassengerName());
        assistance.setAssistanceType(request.getAssistanceType());

        // Auto-assign the creator as the agent & mark as Assigned directly
        assistance.setAssignedAgent(agent);
        assistance.setStatus(AssistanceStatus.Assigned);

        SpecialAssistance saved = assistanceRepository.save(assistance);
        log.info("Successfully created and assigned special assistance request ID {}", saved.getAssistanceId());

        // Audit Logging
        auditLogService.log(agent.getUserId(), "CREATED_SPECIAL_ASSISTANCE", ASSISTANCE_ENTITY_TYPE);

        // Send a notification directly to the creating agent
        notificationService.sendNotification(
                agent.getUserId(),
                "Assistance request created & assigned: " + request.getAssistanceType()
                        + " for " + request.getPassengerName()
                        + " on flight " + flight.getFlightNumber(),
                NotificationCategory.Passenger
        );

        return toAssistanceResponse(saved);
    }

    @Transactional
    public List<SpecialAssistanceResponse> getAllAssistanceRequests() {
        log.debug("Fetching all special assistance requests ordered by status");
        return assistanceRepository.findAllByOrderByStatusAsc()
                .stream()
                .map(this::toAssistanceResponse)
                .toList();
    }

    @Transactional
    public SpecialAssistanceResponse completeAssistance(String assistanceId) {
        log.info("Attempting to complete assistance request ID {}", assistanceId);
        SpecialAssistance assistance = findAssistanceById(assistanceId);

        if (assistance.getStatus() != AssistanceStatus.Assigned) {
            log.warn("Bad Request: Assistance request ID {} is in '{}' status, must be 'Assigned' to complete.",
                    assistanceId, assistance.getStatus());
            throw new BadRequestException("Assistance must be Assigned before it can be marked complete");
        }

        assistance.setStatus(AssistanceStatus.Completed);
        SpecialAssistance saved = assistanceRepository.save(assistance);
        log.info("Assistance request ID {} successfully marked COMPLETED", assistanceId);

        // Audit Logging
        User currentUser = getCurrentUser();
        auditLogService.log(currentUser.getUserId(), "COMPLETED_SPECIAL_ASSISTANCE", ASSISTANCE_ENTITY_TYPE);

        return toAssistanceResponse(saved);
    }

    public List<SpecialAssistanceResponse> getAllAssistanceRequestsByUserId(String userId) {
        return assistanceRepository.findByAssignedAgent_UserId(userId).stream().map(this::toAssistanceResponse).toList();
    }

    public List<SpecialAssistanceResponse> getAssistanceRequestsByFlight(String flightId) {
        log.debug("Fetching special assistance requests for flight ID {}", flightId);
        return assistanceRepository.findByFlight_FlightId(flightId)
                .stream().map(this::toAssistanceResponse).toList();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user profile not found"));
    }

    private User findAgent(String agentId) {
        return userRepository.findById(agentId)
                .orElseThrow(() -> {
                    log.error("ResourceNotFoundException: Agent with ID {} not found", agentId);
                    return new ResourceNotFoundException("Agent not found: " + agentId);
                });
    }

    private CheckInCounter findCounterById(String id) {
        return counterRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("ResourceNotFoundException: Check-in Counter with ID {} not found", id);
                    return new ResourceNotFoundException("Counter not found: " + id);
                });
    }

    private BoardingGate findGateById(String id) {
        return gateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("ResourceNotFoundException: Boarding Gate with ID {} not found", id);
                    return new ResourceNotFoundException("Gate not found: " + id);
                });
    }

    private SpecialAssistance findAssistanceById(String id) {
        return assistanceRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("ResourceNotFoundException: Special Assistance request with ID {} not found", id);
                    return new ResourceNotFoundException("Assistance request not found: " + id);
                });
    }

    // ─── Mappers ────────────────────────────────────────────────────────────────

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