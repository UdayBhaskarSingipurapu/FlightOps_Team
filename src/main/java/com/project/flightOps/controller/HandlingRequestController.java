package com.project.flightOps.controller;

import com.project.flightOps.requestdto.HandlingRequestDto;
import com.project.flightOps.requestdto.HandlingStatusRequest;
import com.project.flightOps.responsedto.HandlingRequestResponse;
import com.project.flightOps.service.HandlingRequestService;
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
@RequestMapping("/api/handling-requests")
@RequiredArgsConstructor
public class HandlingRequestController {

    private final HandlingRequestService handlingRequestService;

    @PostMapping
    @PreAuthorize("hasRole('AirlineCoordinator')")
    public ResponseEntity<ApiResponse<HandlingRequestResponse>> create(
            @Valid @RequestBody HandlingRequestDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Handling request submitted",
                        handlingRequestService.create(dto, userDetails.getUsername())));
    }

    // GET /api/handling-requests?airline=AI for coordinator; no param = all for supervisor
    @GetMapping
    @PreAuthorize("hasAnyRole('AirlineCoordinator', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<List<HandlingRequestResponse>>> getAll(
            @RequestParam(required = false) String airline) {
        List<HandlingRequestResponse> result = (airline != null)
                ? handlingRequestService.getByAirline(airline)
                : handlingRequestService.getAll();
        return ResponseEntity.ok(ApiResponse.success("Handling requests fetched", result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AirlineCoordinator', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<HandlingRequestResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Handling request fetched",
                handlingRequestService.getById(id)));
    }

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<HandlingRequestResponse>>> getByFlight(
            @PathVariable String flightId) {
        return ResponseEntity.ok(ApiResponse.success("Handling requests for flight fetched",
                handlingRequestService.getByFlight(flightId)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('GroundSupervisor')")
    public ResponseEntity<ApiResponse<HandlingRequestResponse>> updateStatus(
            @PathVariable String id, @Valid @RequestBody HandlingStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Status updated",
                handlingRequestService.updateStatus(id, request)));
    }
}
