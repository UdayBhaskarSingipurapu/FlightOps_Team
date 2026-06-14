package com.project.flightOps.service;

import com.project.flightOps.entity.BaggageOperation;
import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.MishandledBaggage;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.MishandledStatus;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // Added Lombok SLF4J annotation
public class BaggageService {

    private final BaggageOperationRepository baggageOperationRepository;
    private final MishandledBaggageRepository mishandledBaggageRepository;
    private final FlightService flightService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ─── Baggage Operations ─────────────────────────────────────────────────────

    @Transactional
    public BaggageOperationResponse createOperation(BaggageOperationRequest request,
                                                    String operatorUserId) {
        log.info("Attempting to create baggage operation for flight ID: {}, direction: {}",
                request.getFlightId(), request.getDirection());

        Flight flight = flightService.findById(request.getFlightId());

        // Prevent duplicate Inbound/Outbound op for same flight
        if (baggageOperationRepository.existsByFlight_FlightIdAndDirection(
                request.getFlightId(), request.getDirection())) {
            log.warn("Conflict: A {} baggage operation already exists for flight {}",
                    request.getDirection(), flight.getFlightNumber());
            throw new ConflictException("A " + request.getDirection()
                    + " baggage operation already exists for flight "
                    + flight.getFlightNumber());
        }

        User operator = userRepository.findByEmail(operatorUserId)
                .orElseThrow(() -> {
                    log.error("Operator not found with ID: {}", operatorUserId);
                    return new ResourceNotFoundException("Operator not found");
                });

        BaggageOperation operation = new BaggageOperation();
        operation.setFlight(flight);
        operation.setDirection(request.getDirection());
        operation.setTotalBagsExpected(request.getTotalBagsExpected());
        operation.setTotalBagsProcessed(0);
        operation.setOperator(operator);
        operation.setStartTime(request.getStartTime());
        operation.setStatus(OperationStatus.InProgress);

        BaggageOperation saved = baggageOperationRepository.save(operation);
        log.info("Successfully created baggage operation with ID: {} for flight: {}",
                saved.getOperationId(), flight.getFlightNumber());

        return toOperationResponse(saved);
    }

    public List<BaggageOperationResponse> getAllOperations() {
        log.debug("Fetching all baggage operations");
        return baggageOperationRepository.findAllByOrderByStartTimeDesc()
                .stream().map(this::toOperationResponse).toList();
    }

    public List<BaggageOperationResponse> getOperationsByFlight(String flightId) {
        log.debug("Fetching baggage operations for flight ID: {}", flightId);
        return baggageOperationRepository.findByFlight_FlightIdOrderByStartTimeDesc(flightId)
                .stream().map(this::toOperationResponse).toList();
    }

    @Transactional
    public BaggageOperationResponse updateCount(String operationId, BaggageCountRequest request) {
        log.info("Updating baggage count for operation ID: {}, new processed count: {}",
                operationId, request.getTotalBagsProcessed());

        BaggageOperation operation = findOperationById(operationId);

        if (operation.getStatus() == OperationStatus.Completed) {
            log.warn("Update rejected: Operation {} is already completed", operationId);
            throw new BadRequestException("Cannot update a completed baggage operation");
        }
        if (request.getTotalBagsProcessed() > operation.getTotalBagsExpected()) {
            log.warn("Update rejected: Processed count {} exceeds expected count {}",
                    request.getTotalBagsProcessed(), operation.getTotalBagsExpected());
            throw new BadRequestException("Bags processed (" + request.getTotalBagsProcessed()
                    + ") cannot exceed bags expected (" + operation.getTotalBagsExpected() + ")");
        }

        operation.setTotalBagsProcessed(request.getTotalBagsProcessed());
        return toOperationResponse(baggageOperationRepository.save(operation));
    }

    @Transactional
    public BaggageOperationResponse completeOperation(String operationId) {
        log.info("Attempting to complete baggage operation ID: {}", operationId);
        BaggageOperation operation = findOperationById(operationId);

        if (operation.getStatus() == OperationStatus.Completed) {
            log.warn("Completion rejected: Operation {} is already completed", operationId);
            throw new BadRequestException("Baggage operation is already completed");
        }

        operation.setEndTime(LocalDateTime.now());
        int discrepancy = operation.getTotalBagsExpected() - operation.getTotalBagsProcessed();

        if (discrepancy != 0) {
            log.warn("Baggage discrepancy detected for operation {}: {} bags unaccounted",
                    operationId, discrepancy);
            operation.setStatus(OperationStatus.Discrepancy);

            // Notify supervisors and ramp officers of discrepancy
            List.of(Role.GroundSupervisor, Role.RampOfficer).forEach(role ->
                    userRepository.findByRole(role).forEach(u -> {
                        log.debug("Sending discrepancy notification to user: {} (Role: {})", u.getUserId(), role);
                        notificationService.sendNotification(u.getUserId(),
                                "Baggage discrepancy on flight "
                                        + operation.getFlight().getFlightNumber()
                                        + ": " + discrepancy + " bags unaccounted ("
                                        + operation.getDirection() + ")",
                                NotificationCategory.Baggage);
                    }));
        } else {
            log.info("Operation {} completed successfully with zero discrepancy", operationId);
            operation.setStatus(OperationStatus.Completed);
        }

        return toOperationResponse(baggageOperationRepository.save(operation));
    }

    // ─── Mishandled Baggage ─────────────────────────────────────────────────────

    @Transactional
    public MishandledBaggageResponse reportMishandled(MishandledBaggageRequest request) {
        log.info("Reporting mishandled baggage. Tag: {}, Passenger: {}",
                request.getBagTagNumber(), request.getPassengerName());

        Flight flight = flightService.findById(request.getFlightId());

        // Prevent duplicate bag tag reports
        mishandledBaggageRepository.findByBagTagNumber(request.getBagTagNumber())
                .ifPresent(existing -> {
                    log.warn("Conflict: Bag tag {} is already reported", request.getBagTagNumber());
                    throw new ConflictException("Bag tag " + request.getBagTagNumber()
                            + " is already reported");
                });

        MishandledBaggage mishandled = new MishandledBaggage();
        mishandled.setFlight(flight);
        mishandled.setPassengerName(request.getPassengerName());
        mishandled.setBagTagNumber(request.getBagTagNumber());
        mishandled.setMishandleType(request.getMishandleType());
        mishandled.setStatus(MishandledStatus.Reported);

        MishandledBaggage saved = mishandledBaggageRepository.save(mishandled);
        log.info("Mishandled baggage recorded with ID: {}", saved.getMishandleId());

        // Notify supervisors
        userRepository.findByRole(Role.GroundSupervisor).forEach(u -> {
            log.debug("Sending mishandled baggage notification to Supervisor: {}", u.getUserId());
            notificationService.sendNotification(u.getUserId(),
                    "Mishandled bag reported: " + request.getMishandleType()
                            + " — Tag: " + request.getBagTagNumber()
                            + " — Passenger: " + request.getPassengerName()
                            + " — Flight: " + flight.getFlightNumber(),
                    NotificationCategory.Baggage);
        });

        return toMishandledResponse(saved);
    }

    public List<MishandledBaggageResponse> getAllMishandled() {
        log.debug("Fetching all mishandled baggage records");
        return mishandledBaggageRepository.findAllByOrderByReportedDateDesc()
                .stream().map(this::toMishandledResponse).toList();
    }

    public MishandledBaggageResponse getByBagTag(String bagTagNumber) {
        log.debug("Fetching mishandled record for bag tag: {}", bagTagNumber);
        MishandledBaggage bag = mishandledBaggageRepository.findByBagTagNumber(bagTagNumber)
                .orElseThrow(() -> {
                    log.warn("Mishandled record not found for bag tag: {}", bagTagNumber);
                    return new ResourceNotFoundException("No mishandled record for bag tag: " + bagTagNumber);
                });
        return toMishandledResponse(bag);
    }

    @Transactional
    public MishandledBaggageResponse updateMishandledStatus(String mishandleId,
                                                            MishandledStatusRequest request) {
        log.info("Updating status of mishandled baggage record {} to {}", mishandleId, request.getStatus());

        MishandledBaggage bag = mishandledBaggageRepository.findById(mishandleId)
                .orElseThrow(() -> {
                    log.error("Mishandled baggage record not found: {}", mishandleId);
                    return new ResourceNotFoundException("Mishandled baggage record not found: " + mishandleId);
                });

        if (bag.getStatus() == MishandledStatus.ClosedUnresolved
                || bag.getStatus() == MishandledStatus.Claimed) {
            log.warn("Update rejected: Cannot modify closed/claimed case {}", mishandleId);
            throw new BadRequestException("Cannot update a closed/claimed baggage case");
        }

        bag.setStatus(request.getStatus());
        return toMishandledResponse(mishandledBaggageRepository.save(bag));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private BaggageOperation findOperationById(String id) {
        return baggageOperationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Baggage operation not found for ID: {}", id);
                    return new ResourceNotFoundException("Baggage operation not found: " + id);
                });
    }

    private BaggageOperationResponse toOperationResponse(BaggageOperation op) {
        int discrepancy = op.getTotalBagsExpected()
                - (op.getTotalBagsProcessed() != null ? op.getTotalBagsProcessed() : 0);
        return BaggageOperationResponse.builder()
                .operationId(op.getOperationId())
                .flightId(op.getFlight().getFlightId())
                .flightNumber(op.getFlight().getFlightNumber())
                .direction(op.getDirection())
                .totalBagsExpected(op.getTotalBagsExpected())
                .totalBagsProcessed(op.getTotalBagsProcessed())
                .discrepancy(discrepancy)
                .operatorId(op.getOperator() != null ? op.getOperator().getUserId() : null)
                .operatorName(op.getOperator() != null ? op.getOperator().getName() : null)
                .startTime(op.getStartTime())
                .endTime(op.getEndTime())
                .status(op.getStatus())
                .build();
    }

    private MishandledBaggageResponse toMishandledResponse(MishandledBaggage m) {
        return MishandledBaggageResponse.builder()
                .mishandleId(m.getMishandleId())
                .flightId(m.getFlight().getFlightId())
                .flightNumber(m.getFlight().getFlightNumber())
                .passengerName(m.getPassengerName())
                .bagTagNumber(m.getBagTagNumber())
                .mishandleType(m.getMishandleType())
                .reportedDate(m.getReportedDate())
                .status(m.getStatus())
                .build();
    }
}
