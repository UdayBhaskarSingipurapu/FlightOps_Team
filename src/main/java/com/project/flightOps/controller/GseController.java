package com.project.flightOps.controller;

import com.project.flightOps.requestdto.EquipmentAllocationRequest;
import com.project.flightOps.requestdto.EquipmentMaintenanceRequest;
import com.project.flightOps.requestdto.EquipmentStatusRequest;
import com.project.flightOps.requestdto.GroundEquipmentRequest;
import com.project.flightOps.responsedto.EquipmentAllocationResponse;
import com.project.flightOps.responsedto.EquipmentMaintenanceResponse;
import com.project.flightOps.responsedto.GroundEquipmentResponse;
import com.project.flightOps.service.GseService;
import com.project.flightOps.util.ApiResponse;
import com.project.flightOps.util.PageResponse;
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
@RequiredArgsConstructor
@Slf4j // Enables the 'log' object via Lombok
public class GseController {

    private final GseService gseService;

    // ─── Equipment ──────────────────────────────────────────────────────────────

    @PostMapping("/api/equipment")
    @PreAuthorize("hasAnyRole('Admin', 'GSEManager')")
    public ResponseEntity<ApiResponse<GroundEquipmentResponse>> register(
            @Valid @RequestBody GroundEquipmentRequest request) {
        log.info("Received request to register new ground equipment");
        log.debug("Equipment registration payload: {}", request);

        GroundEquipmentResponse response = gseService.registerEquipment(request);

        log.info("Successfully registered equipment");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Equipment registered", response));
    }

    @GetMapping("/api/equipment")
    @PreAuthorize("hasAnyRole('GSEManager', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<PageResponse<GroundEquipmentResponse>>> getAllEquipments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching ground equipment list - page: {}, limit: {}", page, limit);
        PageResponse<GroundEquipmentResponse> response = gseService.getAllEquipment(page, limit);
        log.info("Successfully fetched {} of {} equipment items", response.getData().size(), response.getTotalCount());
        return ResponseEntity.ok(ApiResponse.success("Equipment list fetched", response));
    }

    @GetMapping("/api/equipment/{id}")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<GroundEquipmentResponse>> getById(@PathVariable String id) {
        log.info("Fetching ground equipment details for ID: {}", id);
        GroundEquipmentResponse response = gseService.getEquipmentById(id);
        log.info("Successfully fetched ground equipment details for ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Equipment fetched", response));
    }

    @GetMapping("/api/equipment/available")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<List<GroundEquipmentResponse>>> getAvailable() {
        log.info("Fetching all available ground equipment");
        List<GroundEquipmentResponse> response = gseService.getAvailableEquipment();
        log.info("Successfully fetched {} available equipment items", response.size());
        return ResponseEntity.ok(ApiResponse.success("Available equipment fetched", response));
    }

    @PatchMapping("/api/equipment/{id}/status")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<GroundEquipmentResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody EquipmentStatusRequest request) {
        log.info("Received request to update status for equipment ID: {}", id);
        log.debug("Equipment status update payload: {}", request);

        GroundEquipmentResponse response = gseService.updateEquipmentStatus(id, request);

        log.info("Successfully updated status for equipment ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Equipment status updated", response));
    }

    // ─── Allocations ────────────────────────────────────────────────────────────

    @PostMapping("/api/allocations")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<EquipmentAllocationResponse>> allocate(
            @Valid @RequestBody EquipmentAllocationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Received request to allocate equipment by user: {}", userDetails.getUsername());
        log.debug("Equipment allocation payload: {}", request);

        EquipmentAllocationResponse response = gseService.allocate(request, userDetails.getUsername());

        log.info("Successfully allocated equipment");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Equipment allocated", response));
    }

    @GetMapping("/api/allocations/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<EquipmentAllocationResponse>>> getByFlight(
            @PathVariable String flightId) {
        log.info("Fetching equipment allocations for flight ID: {}", flightId);
        List<EquipmentAllocationResponse> response = gseService.getAllocationsByFlight(flightId);
        log.info("Successfully fetched {} allocations for flight ID: {}", response.size(), flightId);
        return ResponseEntity.ok(ApiResponse.success("Allocations for flight fetched", response));
    }

    @GetMapping("/api/allocations")
    @PreAuthorize("hasAnyRole('GSEManager', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<List<EquipmentAllocationResponse>>> getActive() {
        log.info("Fetching all active equipment allocations");
        List<EquipmentAllocationResponse> response = gseService.getAllActiveAllocations();
        log.info("Successfully fetched {} active allocations", response.size());
        return ResponseEntity.ok(ApiResponse.success("Active allocations fetched", response));
    }

    @GetMapping("/api/allocations/user/{userId}")
    @PreAuthorize("hasAnyRole('GSEManager', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<List<EquipmentAllocationResponse>>> getActiveByUser(@PathVariable String userId) {
        log.info("Fetching all active equipment allocations");
        List<EquipmentAllocationResponse> response = gseService.getAllActiveAllocationsByUser(userId);
        log.info("Successfully fetched {} active allocations", response.size());
        return ResponseEntity.ok(ApiResponse.success("Active allocations fetched", response));
    }

    @PatchMapping("/api/allocations/{id}/release")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<EquipmentAllocationResponse>> release(@PathVariable String id) {
        log.info("Received request to release equipment allocation ID: {}", id);
        EquipmentAllocationResponse response = gseService.release(id);
        log.info("Successfully released equipment allocation ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Equipment released", response));
    }

    // ─── Maintenance ────────────────────────────────────────────────────────────

    @PostMapping("/api/maintenance")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<EquipmentMaintenanceResponse>> reportFault(
            @Valid @RequestBody EquipmentMaintenanceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Received maintenance fault report by user: {}", userDetails.getUsername());
        log.debug("Maintenance fault report payload: {}", request);

        EquipmentMaintenanceResponse response = gseService.reportMaintenance(request, userDetails.getUsername());

        log.info("Successfully logged maintenance fault report");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Maintenance reported", response));
    }

    @GetMapping("/api/maintenance")
    @PreAuthorize("hasAnyRole('GSEManager', 'Admin')")
    public ResponseEntity<ApiResponse<List<EquipmentMaintenanceResponse>>> getAllMaintenanceRecords() {
        log.info("Fetching all equipment maintenance records");
        List<EquipmentMaintenanceResponse> response = gseService.getAllMaintenance();
        log.info("Successfully fetched {} maintenance records", response.size());
        return ResponseEntity.ok(ApiResponse.success("Maintenance records fetched", response));
    }

    @PatchMapping("/api/maintenance/{id}/resolve")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<EquipmentMaintenanceResponse>> resolve(@PathVariable String id) {
        log.info("Received request to resolve maintenance record ID: {}", id);
        EquipmentMaintenanceResponse response = gseService.resolveMaintenence(id);
        log.info("Successfully resolved maintenance and returned equipment ID: {} to service", id);
        return ResponseEntity.ok(ApiResponse.success("Equipment returned to service", response));
    }
}