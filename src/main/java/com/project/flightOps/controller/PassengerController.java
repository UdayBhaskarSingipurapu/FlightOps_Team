package com.project.flightOps.controller;

import com.project.flightOps.requestdto.*;
import com.project.flightOps.responsedto.BoardingGateResponse;
import com.project.flightOps.responsedto.CheckInCounterResponse;
import com.project.flightOps.responsedto.SpecialAssistanceResponse;
import com.project.flightOps.service.PassengerService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PassengerController {

    private final PassengerService passengerService;

    // ─── Check-in Counters ──────────────────────────────────────────────────────

    @PostMapping("/api/counters")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<CheckInCounterResponse>> assignCounter(
            @Valid @RequestBody CheckInCounterRequest request) {
        log.info("Received request to assign check-in counter");
        CheckInCounterResponse response = passengerService.assignCounter(request);
        log.info("Successfully assigned check-in counter");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Counter assigned", response));
    }

    @GetMapping("/api/counters")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<List<CheckInCounterResponse>>> getAllCounters() {
        log.info("Fetching all check-in counters");
        List<CheckInCounterResponse> response = passengerService.getAllCounters();
        log.debug("Fetched {} check-in counters", response.size());
        return ResponseEntity.ok(ApiResponse.success("Counters fetched", response));
    }

    @GetMapping("/api/counters/byUser/{userId}")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<List<CheckInCounterResponse>>> getCountersByUserId(@PathVariable String userId){
        log.info("fetching all check-in counters assigned by passenger agent with id {}", userId);
        List<CheckInCounterResponse> responses = passengerService.getAllCountersAssignedByAgent(userId);
        log.debug("Fetched {} check-in counters assigned by agent", responses);
        return ResponseEntity.ok(ApiResponse.success("Counter by agent fetched successfully", responses));
    }

    @GetMapping("/api/counters/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<CheckInCounterResponse>>> getByFlight(
            @PathVariable String flightId) {
        log.info("Fetching check-in counters for flight: {}", flightId);
        List<CheckInCounterResponse> response = passengerService.getCountersByFlight(flightId);
        log.debug("Found {} counters for flight: {}", response.size(), flightId);
        return ResponseEntity.ok(ApiResponse.success("Counters for flight fetched", response));
    }

    @PatchMapping("/api/counters/{id}/status")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<CheckInCounterResponse>> updateCounterStatus(
            @PathVariable String id,
            @Valid @RequestBody CounterStatusRequest request) {
        log.info("Updating status of counter ID: {}", id);
        CheckInCounterResponse response = passengerService.updateCounterStatus(id, request);
        log.info("Successfully updated status of counter ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Counter status updated", response));
    }

    // ─── Boarding Gates ──────────────────────────────────────────────────────────

    @PostMapping("/api/gates")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<BoardingGateResponse>> assignGate(
            @Valid @RequestBody BoardingGateRequest request) {
        log.info("Received request to assign boarding gate");
        BoardingGateResponse response = passengerService.assignGate(request);
        log.info("Successfully assigned boarding gate");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gate assigned", response));
    }

    @GetMapping("/api/gates")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<List<BoardingGateResponse>>> getAllGates() {
        log.info("Fetching all boarding gates");
        List<BoardingGateResponse> response = passengerService.getAllGates();
        log.debug("Fetched {} boarding gates", response.size());
        return ResponseEntity.ok(ApiResponse.success("Gates fetched", response));
    }

    @GetMapping("/api/gates/byUser/{userId}")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<List<BoardingGateResponse>>> getBoardingGatesByUserId(@PathVariable String userId){
        log.info("fetching gates assigned by passenger agent with id {}", userId);
        List<BoardingGateResponse> responses = passengerService.getAllBoardingGatesAssignedByAgent(userId);
        log.debug("Fetched {} gates assigned by agent", responses);
        return ResponseEntity.ok(ApiResponse.success("Counter by agent fetched successfully", responses));
    }

    @GetMapping("/api/gates/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<BoardingGateResponse>>> getGatesByFlight(
            @PathVariable String flightId) {
        log.info("Fetching boarding gates for flight: {}", flightId);
        List<BoardingGateResponse> response = passengerService.getGatesByFlight(flightId);
        log.debug("Found {} gates for flight: {}", response.size(), flightId);
        return ResponseEntity.ok(ApiResponse.success("Gates for flight fetched", response));
    }

    @PatchMapping("/api/gates/{id}/status")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<BoardingGateResponse>> updateGateStatus(
            @PathVariable String id,
            @Valid @RequestBody GateStatusRequest request) {
        log.info("Updating status of gate ID: {}", id);
        BoardingGateResponse response = passengerService.updateGateStatus(id, request);
        log.info("Successfully updated status of gate ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Gate status updated", response));
    }

    // ─── Special Assistance ──────────────────────────────────────────────────────

    @PostMapping("/api/special-assistance")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<SpecialAssistanceResponse>> createRequest(
            @Valid @RequestBody SpecialAssistanceRequest request) {
        log.info("Received request to create special assistance");
        SpecialAssistanceResponse response = passengerService.createAssistanceRequest(request);
        log.info("Successfully created assistance request");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assistance request created", response));
    }

    @GetMapping("/api/special-assistance")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<List<SpecialAssistanceResponse>>> getAll() {
        log.info("Fetching all special assistance requests");
        List<SpecialAssistanceResponse> response = passengerService.getAllAssistanceRequests();
        log.debug("Fetched {} special assistance requests", response.size());
        return ResponseEntity.ok(ApiResponse.success("Assistance requests fetched", response));
    }

    @GetMapping("/api/special-assistance/byUser/{userId}")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<List<SpecialAssistanceResponse>>> getAllByUserId(@PathVariable String userId) {
        log.info("Fetching all special assistance requests by user id {}", userId);
        List<SpecialAssistanceResponse> response = passengerService.getAllAssistanceRequestsByUserId(userId);
        log.debug("Fetched {} special assistance requests", response);
        return ResponseEntity.ok(ApiResponse.success("Assistance requests fetched", response));
    }

    @PatchMapping("/api/special-assistance/{id}/complete")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<SpecialAssistanceResponse>> complete(@PathVariable String id) {
        // Fixed: Removed the response.getId() call and used the 'id' path variable instead
        log.info("Completing special assistance request ID: {}", id);
        SpecialAssistanceResponse response = passengerService.completeAssistance(id);
        log.info("Successfully completed assistance request ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Assistance completed", response));
    }
}