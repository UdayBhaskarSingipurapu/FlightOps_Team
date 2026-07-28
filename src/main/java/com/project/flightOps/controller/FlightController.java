package com.project.flightOps.controller;

import com.project.flightOps.requestdto.FlightRequest;
import com.project.flightOps.requestdto.FlightStatusRequest;
import com.project.flightOps.responsedto.FlightResponse;
import com.project.flightOps.service.FlightService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
@Slf4j // Enables the 'log' object via Lombok
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<FlightResponse>> create(
            @Valid @RequestBody FlightRequest request) {
        log.info("Received request to create a new flight");
        log.debug("Flight creation payload: {}", request);

        FlightResponse response = flightService.create(request);

        log.info("Successfully created flight");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flight created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightResponse>>> getAll(
            @RequestParam(required = false) String airline) {
        if (airline != null) {
            log.info("Fetching flights filtered by airline: {}", airline);
        } else {
            log.info("Fetching today's flight schedule");
        }

        List<FlightResponse> result = (airline != null)
                ? flightService.getByAirline(airline)
                : flightService.getToday();

        log.info("Successfully fetched {} flights", result.size());
        return ResponseEntity.ok(ApiResponse.success("Flights fetched", result));
    }

    @GetMapping("/allByHandlingService")
    public ResponseEntity<ApiResponse<List<FlightResponse>>> getAllByHandlingRequestType(
            @RequestParam(required = true) String serviceType
    ) {
        List<FlightResponse> responses = flightService.getAllFlightsWithHandlingRequestServiceType(serviceType);
        return ResponseEntity.ok(ApiResponse.success("Flights fetched", responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightResponse>> getById(@PathVariable String id) {
        log.info("Fetching flight details for ID: {}", id);
        FlightResponse response = flightService.getById(id);
        log.info("Successfully fetched flight details for ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Flight fetched", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<FlightResponse>> update(
            @PathVariable String id, @Valid @RequestBody FlightRequest request) {
        log.info("Received request to update flight ID: {}", id);
        log.debug("Flight update payload: {}", request);

        FlightResponse response = flightService.update(id, request);

        log.info("Successfully updated flight ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Flight updated", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<FlightResponse>> updateStatus(
            @PathVariable String id, @Valid @RequestBody FlightStatusRequest request) {
        log.info("Received request to update status for flight ID: {}", id);
        log.debug("Flight status update payload: {}", request);

        FlightResponse response = flightService.updateStatus(id, request);

        log.info("Successfully updated status for flight ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Flight status updated", response));
    }
}