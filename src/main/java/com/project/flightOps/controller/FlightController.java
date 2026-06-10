package com.project.flightOps.controller;

import com.project.flightOps.requestdto.FlightRequest;
import com.project.flightOps.requestdto.FlightStatusRequest;
import com.project.flightOps.responsedto.FlightResponse;
import com.project.flightOps.service.FlightService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<FlightResponse>> create(
            @Valid @RequestBody FlightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flight created", flightService.create(request)));
    }

    // GET /api/flights — returns today's schedule; add ?airline=AI for filter
    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightResponse>>> getAll(
            @RequestParam(required = false) String airline) {
        List<FlightResponse> result = (airline != null)
                ? flightService.getByAirline(airline)
                : flightService.getToday();
        return ResponseEntity.ok(ApiResponse.success("Flights fetched", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Flight fetched", flightService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<FlightResponse>> update(
            @PathVariable String id, @Valid @RequestBody FlightRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Flight updated", flightService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<FlightResponse>> updateStatus(
            @PathVariable String id, @Valid @RequestBody FlightStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Flight status updated",
                flightService.updateStatus(id, request)));
    }
}
