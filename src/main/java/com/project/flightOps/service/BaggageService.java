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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
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
        Flight flight = flightService.findById(request.getFlightId());

        // Prevent duplicate Inbound/Outbound op for same flight
        if (baggageOperationRepository.existsByFlight_FlightIdAndDirection(
                request.getFlightId(), request.getDirection())) {
            throw new ConflictException("A " + request.getDirection()
                    + " baggage operation already exists for flight "
                    + flight.getFlightNumber());
        }

        User operator = userRepository.findById(operatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));

        BaggageOperation operation = new BaggageOperation();
        operation.setFlight(flight);
        operation.setDirection(request.getDirection());
        operation.setTotalBagsExpected(request.getTotalBagsExpected());
        operation.setTotalBagsProcessed(0);
        operation.setOperator(operator);
        operation.setStartTime(request.getStartTime());
        operation.setStatus(OperationStatus.InProgress);

        return toOperationResponse(baggageOperationRepository.save(operation));
    }

    public List<BaggageOperationResponse> getAllOperations() {
        return baggageOperationRepository.findAllByOrderByStartTimeDesc()
                .stream().map(this::toOperationResponse).toList();
    }

    public List<BaggageOperationResponse> getOperationsByFlight(String flightId) {
        return baggageOperationRepository.findByFlight_FlightIdOrderByStartTimeDesc(flightId)
                .stream().map(this::toOperationResponse).toList();
    }

    @Transactional
    public BaggageOperationResponse updateCount(String operationId, BaggageCountRequest request) {
        BaggageOperation operation = findOperationById(operationId);

        if (operation.getStatus() == OperationStatus.Completed) {
            throw new BadRequestException("Cannot update a completed baggage operation");
        }
        if (request.getTotalBagsProcessed() > operation.getTotalBagsExpected()) {
            throw new BadRequestException("Bags processed (" + request.getTotalBagsProcessed()
                    + ") cannot exceed bags expected (" + operation.getTotalBagsExpected() + ")");
        }

        operation.setTotalBagsProcessed(request.getTotalBagsProcessed());
        return toOperationResponse(baggageOperationRepository.save(operation));
    }

    @Transactional
    public BaggageOperationResponse completeOperation(String operationId) {
        BaggageOperation operation = findOperationById(operationId);

        if (operation.getStatus() == OperationStatus.Completed) {
            throw new BadRequestException("Baggage operation is already completed");
        }

        operation.setEndTime(LocalDateTime.now());

        int discrepancy = operation.getTotalBagsExpected() - operation.getTotalBagsProcessed();

        if (discrepancy != 0) {
            operation.setStatus(OperationStatus.Discrepancy);

            // Notify supervisors and ramp officers of discrepancy
            List.of(Role.GroundSupervisor, Role.RampOfficer).forEach(role ->
                    userRepository.findByRole(role).forEach(u ->
                            notificationService.sendNotification(u.getUserId(),
                                    "Baggage discrepancy on flight "
                                            + operation.getFlight().getFlightNumber()
                                            + ": " + discrepancy + " bags unaccounted ("
                                            + operation.getDirection() + ")",
                                    NotificationCategory.Baggage)));
        } else {
            operation.setStatus(OperationStatus.Completed);
        }

        return toOperationResponse(baggageOperationRepository.save(operation));
    }

    // ─── Mishandled Baggage ─────────────────────────────────────────────────────

    @Transactional
    public MishandledBaggageResponse reportMishandled(MishandledBaggageRequest request) {
        Flight flight = flightService.findById(request.getFlightId());

        // Prevent duplicate bag tag reports
        mishandledBaggageRepository.findByBagTagNumber(request.getBagTagNumber())
                .ifPresent(existing -> {
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

        // Notify supervisors
        userRepository.findByRole(Role.GroundSupervisor).forEach(u ->
                notificationService.sendNotification(u.getUserId(),
                        "Mishandled bag reported: " + request.getMishandleType()
                                + " — Tag: " + request.getBagTagNumber()
                                + " — Passenger: " + request.getPassengerName()
                                + " — Flight: " + flight.getFlightNumber(),
                        NotificationCategory.Baggage));

        return toMishandledResponse(saved);
    }

    public List<MishandledBaggageResponse> getAllMishandled() {
        return mishandledBaggageRepository.findAllByOrderByReportedDateDesc()
                .stream().map(this::toMishandledResponse).toList();
    }

    public MishandledBaggageResponse getByBagTag(String bagTagNumber) {
        MishandledBaggage bag = mishandledBaggageRepository.findByBagTagNumber(bagTagNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No mishandled record for bag tag: " + bagTagNumber));
        return toMishandledResponse(bag);
    }

    @Transactional
    public MishandledBaggageResponse updateMishandledStatus(String mishandleId,
            MishandledStatusRequest request) {
        MishandledBaggage bag = mishandledBaggageRepository.findById(mishandleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mishandled baggage record not found: " + mishandleId));

        if (bag.getStatus() == MishandledStatus.ClosedUnresolved
                || bag.getStatus() == MishandledStatus.Claimed) {
            throw new BadRequestException("Cannot update a closed/claimed baggage case");
        }

        bag.setStatus(request.getStatus());
        return toMishandledResponse(mishandledBaggageRepository.save(bag));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private BaggageOperation findOperationById(String id) {
        return baggageOperationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Baggage operation not found: " + id));
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
