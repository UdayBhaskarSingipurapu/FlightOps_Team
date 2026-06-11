package com.project.flightOps.controller;

import com.project.flightOps.requestdto.BaggageCountRequest;
import com.project.flightOps.requestdto.BaggageOperationRequest;
import com.project.flightOps.requestdto.MishandledBaggageRequest;
import com.project.flightOps.requestdto.MishandledStatusRequest;
import com.project.flightOps.responsedto.BaggageOperationResponse;
import com.project.flightOps.responsedto.MishandledBaggageResponse;
import com.project.flightOps.service.BaggageService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BaggageController {

    private final BaggageService baggageService;

    // ─── Baggage Operations ─────────────────────────────────────────────────────

    @PostMapping("/api/baggage-ops")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<BaggageOperationResponse>> create(
            @Valid @RequestBody BaggageOperationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Received request to create baggage operation by user: {}", userDetails.getUsername());
        log.debug("Baggage creation payload: {}", request);

        BaggageOperationResponse response = baggageService.createOperation(request, userDetails.getUsername());

        log.info("Successfully created baggage operation");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Baggage operation created", response));
    }

    @GetMapping("/api/baggage-ops")
    @PreAuthorize("hasAnyRole('RampOfficer', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<List<BaggageOperationResponse>>> getAllOperations() {
        log.info("Fetching all baggage operations");
        List<BaggageOperationResponse> response = baggageService.getAllOperations();
        log.info("Successfully fetched {} baggage operations", response.size());
        return ResponseEntity.ok(ApiResponse.success("Baggage operations fetched", response));
    }

    @GetMapping("/api/baggage-ops/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<BaggageOperationResponse>>> getByFlight(
            @PathVariable String flightId) {
        log.info("Fetching baggage operations for flight ID: {}", flightId);
        List<BaggageOperationResponse> response = baggageService.getOperationsByFlight(flightId);
        log.info("Successfully fetched {} baggage operations for flight ID: {}", response.size(), flightId);
        return ResponseEntity.ok(ApiResponse.success("Baggage operations for flight fetched", response));
    }

    @PatchMapping("/api/baggage-ops/{id}/count")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<BaggageOperationResponse>> updateCount(
            @PathVariable String id,
            @Valid @RequestBody BaggageCountRequest request) {
        log.info("Updating bag count for operation ID: {}", id);
        log.debug("Bag count update data: {}", request);

        BaggageOperationResponse response = baggageService.updateCount(id, request);

        log.info("Successfully updated bag count for operation ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Bag count updated", response));
    }

    @PatchMapping("/api/baggage-ops/{id}/complete")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<BaggageOperationResponse>> complete(@PathVariable String id) {
        log.info("Completing baggage operation ID: {}", id);
        BaggageOperationResponse response = baggageService.completeOperation(id);
        log.info("Successfully completed baggage operation ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Baggage operation completed", response));
    }

    // ─── Mishandled Baggage ─────────────────────────────────────────────────────

    @PostMapping("/api/mishandled")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<MishandledBaggageResponse>> report(
            @Valid @RequestBody MishandledBaggageRequest request) {
        log.info("Reporting mishandled baggage");
        log.debug("Mishandled baggage report payload: {}", request);

        MishandledBaggageResponse response = baggageService.reportMishandled(request);

        log.info("Successfully reported mishandled baggage");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mishandled baggage reported", response));
    }

    @GetMapping("/api/mishandled")
    @PreAuthorize("hasAnyRole('RampOfficer', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<List<MishandledBaggageResponse>>> getAllMishandled() {
        log.info("Fetching all mishandled baggage records");
        List<MishandledBaggageResponse> response = baggageService.getAllMishandled();
        log.info("Successfully fetched {} mishandled baggage records", response.size());
        return ResponseEntity.ok(ApiResponse.success("Mishandled bags fetched", response));
    }

    @GetMapping("/api/mishandled/{bagTag}")
    @PreAuthorize("hasAnyRole('RampOfficer', 'PassengerAgent')")
    public ResponseEntity<ApiResponse<MishandledBaggageResponse>> getByBagTag(
            @PathVariable String bagTag) {
        log.info("Fetching mishandled baggage record for bag tag: {}", bagTag);
        MishandledBaggageResponse response = baggageService.getByBagTag(bagTag);
        log.info("Successfully fetched mishandled baggage record for bag tag: {}", bagTag);
        return ResponseEntity.ok(ApiResponse.success("Mishandled bag fetched", response));
    }

    @PatchMapping("/api/mishandled/{id}/status")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<MishandledBaggageResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody MishandledStatusRequest request) {
        log.info("Updating status for mishandled baggage ID: {}", id);
        log.debug("Mishandled status update data: {}", request);

        MishandledBaggageResponse response = baggageService.updateMishandledStatus(id, request);

        log.info("Successfully updated status for mishandled baggage ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Mishandled bag status updated", response));
    }
}