package com.project.flightOps.controller;

import com.project.flightOps.requestdto.*;
import com.project.flightOps.responsedto.BoardingGateResponse;
import com.project.flightOps.responsedto.CheckInCounterResponse;
import com.project.flightOps.responsedto.SpecialAssistanceResponse;
import com.project.flightOps.service.PassengerService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    // ─── Check-in Counters ──────────────────────────────────────────────────────

    @PostMapping("/api/counters")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<CheckInCounterResponse>> assignCounter(
            @Valid @RequestBody CheckInCounterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Counter assigned",
                        passengerService.assignCounter(request)));
    }

    @GetMapping("/api/counters")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<List<CheckInCounterResponse>>> getAllCounters() {
        return ResponseEntity.ok(ApiResponse.success("Counters fetched",
                passengerService.getAllCounters()));
    }

    @GetMapping("/api/counters/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<CheckInCounterResponse>>> getByFlight(
            @PathVariable String flightId) {
        return ResponseEntity.ok(ApiResponse.success("Counters for flight fetched",
                passengerService.getCountersByFlight(flightId)));
    }

    @PatchMapping("/api/counters/{id}/status")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<CheckInCounterResponse>> updateCounterStatus(
            @PathVariable String id,
            @Valid @RequestBody CounterStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Counter status updated",
                passengerService.updateCounterStatus(id, request)));
    }

    // ─── Boarding Gates ──────────────────────────────────────────────────────────

    @PostMapping("/api/gates")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<BoardingGateResponse>> assignGate(
            @Valid @RequestBody BoardingGateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gate assigned",
                        passengerService.assignGate(request)));
    }

    @GetMapping("/api/gates")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<List<BoardingGateResponse>>> getAllGates() {
        return ResponseEntity.ok(ApiResponse.success("Gates fetched",
                passengerService.getAllGates()));
    }

    @GetMapping("/api/gates/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<BoardingGateResponse>>> getGatesByFlight(
            @PathVariable String flightId) {
        return ResponseEntity.ok(ApiResponse.success("Gates for flight fetched",
                passengerService.getGatesByFlight(flightId)));
    }

    @PatchMapping("/api/gates/{id}/status")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<BoardingGateResponse>> updateGateStatus(
            @PathVariable String id,
            @Valid @RequestBody GateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Gate status updated",
                passengerService.updateGateStatus(id, request)));
    }

    // ─── Special Assistance ──────────────────────────────────────────────────────

    @PostMapping("/api/special-assistance")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<SpecialAssistanceResponse>> createRequest(
            @Valid @RequestBody SpecialAssistanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Assistance request created",
                        passengerService.createAssistanceRequest(request)));
    }

    @GetMapping("/api/special-assistance")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<List<SpecialAssistanceResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Assistance requests fetched",
                passengerService.getAllAssistanceRequests()));
    }

    @PatchMapping("/api/special-assistance/{id}/assign")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<SpecialAssistanceResponse>> assignAgent(
            @PathVariable String id,
            @Valid @RequestBody AssistanceAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Agent assigned",
                passengerService.assignAgent(id, request)));
    }

    @PatchMapping("/api/special-assistance/{id}/complete")
    @PreAuthorize("hasRole('PassengerAgent')")
    public ResponseEntity<ApiResponse<SpecialAssistanceResponse>> complete(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Assistance completed",
                passengerService.completeAssistance(id)));
    }
}
