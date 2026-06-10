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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BaggageController {

    private final BaggageService baggageService;

    // ─── Baggage Operations ─────────────────────────────────────────────────────

    @PostMapping("/api/baggage-ops")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<BaggageOperationResponse>> create(
            @Valid @RequestBody BaggageOperationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Baggage operation created",
                        baggageService.createOperation(request, userDetails.getUsername())));
    }

    @GetMapping("/api/baggage-ops")
    @PreAuthorize("hasAnyRole('RampOfficer', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<List<BaggageOperationResponse>>> getAllOperations() {
        return ResponseEntity.ok(ApiResponse.success("Baggage operations fetched",
                baggageService.getAllOperations()));
    }

    @GetMapping("/api/baggage-ops/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<BaggageOperationResponse>>> getByFlight(
            @PathVariable String flightId) {
        return ResponseEntity.ok(ApiResponse.success("Baggage operations for flight fetched",
                baggageService.getOperationsByFlight(flightId)));
    }

    @PatchMapping("/api/baggage-ops/{id}/count")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<BaggageOperationResponse>> updateCount(
            @PathVariable String id,
            @Valid @RequestBody BaggageCountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bag count updated",
                baggageService.updateCount(id, request)));
    }

    @PatchMapping("/api/baggage-ops/{id}/complete")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<BaggageOperationResponse>> complete(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Baggage operation completed",
                baggageService.completeOperation(id)));
    }

    // ─── Mishandled Baggage ─────────────────────────────────────────────────────

    @PostMapping("/api/mishandled")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<MishandledBaggageResponse>> report(
            @Valid @RequestBody MishandledBaggageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mishandled baggage reported",
                        baggageService.reportMishandled(request)));
    }

    @GetMapping("/api/mishandled")
    @PreAuthorize("hasAnyRole('RampOfficer', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<List<MishandledBaggageResponse>>> getAllMishandled() {
        return ResponseEntity.ok(ApiResponse.success("Mishandled bags fetched",
                baggageService.getAllMishandled()));
    }

    @GetMapping("/api/mishandled/{bagTag}")
    @PreAuthorize("hasAnyRole('RampOfficer', 'PassengerAgent')")
    public ResponseEntity<ApiResponse<MishandledBaggageResponse>> getByBagTag(
            @PathVariable String bagTag) {
        return ResponseEntity.ok(ApiResponse.success("Mishandled bag fetched",
                baggageService.getByBagTag(bagTag)));
    }

    @PatchMapping("/api/mishandled/{id}/status")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<MishandledBaggageResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody MishandledStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Mishandled bag status updated",
                baggageService.updateMishandledStatus(id, request)));
    }
}
