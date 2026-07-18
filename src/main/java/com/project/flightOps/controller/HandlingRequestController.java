package com.project.flightOps.controller;

import com.project.flightOps.requestdto.HandlingRequestDto;
import com.project.flightOps.requestdto.HandlingStatusRequest;
import com.project.flightOps.responsedto.HandlingRequestResponse;
import com.project.flightOps.service.HandlingRequestService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/handling-requests")
@RequiredArgsConstructor
@Slf4j // Enables the 'log' object automatically via Lombok
public class HandlingRequestController {

    private final HandlingRequestService handlingRequestService;

    @PostMapping
    @PreAuthorize("hasRole('AirlineCoordinator')")
    public ResponseEntity<ApiResponse<HandlingRequestResponse>> create(
            @Valid @RequestBody HandlingRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Received request to submit a new handling request by user: {}", userDetails.getUsername());
        log.debug("Handling request payload: {}", dto);

        HandlingRequestResponse response = handlingRequestService.create(dto, userDetails.getUsername());

        log.info("Successfully submitted handling request");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Handling request submitted", response));
    }

    // GET /api/handling-requests?airline=AI for coordinator; no param = all for supervisor
    @GetMapping
    @PreAuthorize("hasAnyRole('AirlineCoordinator', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<List<HandlingRequestResponse>>> getAll(
            @RequestParam(required = false) String airline) {
        if (airline != null) {
            log.info("Fetching handling requests filtered by airline: {}", airline);
        } else {
            log.info("Fetching all handling requests");
        }

        List<HandlingRequestResponse> result = (airline != null)
                ? handlingRequestService.getByAirline(airline)
                : handlingRequestService.getAll();

        log.info("Successfully fetched {} handling requests", result.size());
        return ResponseEntity.ok(ApiResponse.success("Handling requests fetched", result));
    }

    @GetMapping("/byUser/{userId}")
    @PreAuthorize("hasAnyRole('AirlineCoordinator', 'GroundSuperVisor')")
    public ResponseEntity<ApiResponse<List<HandlingRequestResponse>>> getByUserId(@PathVariable String userId){
        log.info("fetching handling requests filtered by userId {}", userId);
        List<HandlingRequestResponse> response = handlingRequestService.getByUserId(userId);
        log.info("Successfully fetched handling requests {} for userId {}", response, userId);
        return ResponseEntity.ok(ApiResponse.success("Handling requests fetched", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AirlineCoordinator', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<HandlingRequestResponse>> getById(@PathVariable String id) {
        log.info("Fetching handling request details for ID: {}", id);
        HandlingRequestResponse response = handlingRequestService.getById(id);
        log.info("Successfully fetched handling request details for ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Handling request fetched", response));
    }

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<HandlingRequestResponse>>> getByFlight(
            @PathVariable String flightId) {
        log.info("Fetching handling requests for flight ID: {}", flightId);
        List<HandlingRequestResponse> response = handlingRequestService.getByFlight(flightId);
        log.info("Successfully fetched {} handling requests for flight ID: {}", response.size(), flightId);
        return ResponseEntity.ok(ApiResponse.success("Handling requests for flight fetched", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('GroundSupervisor')")
    public ResponseEntity<ApiResponse<HandlingRequestResponse>> updateStatus(
            @PathVariable String id, @Valid @RequestBody HandlingStatusRequest request) {
        log.info("Received request to update status for handling request ID: {}", id);
        log.debug("Handling status update payload: {}", request);

        HandlingRequestResponse response = handlingRequestService.updateStatus(id, request);

        log.info("Successfully updated status for handling request ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Status updated", response));
    }
}