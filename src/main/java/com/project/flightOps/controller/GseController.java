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
public class GseController {

    private final GseService gseService;

    // ─── Equipment ──────────────────────────────────────────────────────────────

    @PostMapping("/api/equipment")
    @PreAuthorize("hasAnyRole('Admin', 'GSEManager')")
    public ResponseEntity<ApiResponse<GroundEquipmentResponse>> register(
            @Valid @RequestBody GroundEquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Equipment registered",
                        gseService.registerEquipment(request)));
    }

    @GetMapping("/api/equipment")
    @PreAuthorize("hasAnyRole('GSEManager', 'GroundSupervisor', 'Admin')")
    public ResponseEntity<ApiResponse<List<GroundEquipmentResponse>>> getAllEquipments() {
        return ResponseEntity.ok(ApiResponse.success("Equipment list fetched",
                gseService.getAllEquipment()));
    }

    @GetMapping("/api/equipment/{id}")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<GroundEquipmentResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Equipment fetched",
                gseService.getEquipmentById(id)));
    }

    @GetMapping("/api/equipment/available")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<List<GroundEquipmentResponse>>> getAvailable() {
        return ResponseEntity.ok(ApiResponse.success("Available equipment fetched",
                gseService.getAvailableEquipment()));
    }

    @PatchMapping("/api/equipment/{id}/status")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<GroundEquipmentResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody EquipmentStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Equipment status updated",
                gseService.updateEquipmentStatus(id, request)));
    }

    // ─── Allocations ────────────────────────────────────────────────────────────

    @PostMapping("/api/allocations")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<EquipmentAllocationResponse>> allocate(
            @Valid @RequestBody EquipmentAllocationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Equipment allocated",
                        gseService.allocate(request, userDetails.getUsername())));
    }

    @GetMapping("/api/allocations/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<EquipmentAllocationResponse>>> getByFlight(
            @PathVariable String flightId) {
        return ResponseEntity.ok(ApiResponse.success("Allocations for flight fetched",
                gseService.getAllocationsByFlight(flightId)));
    }

    @GetMapping("/api/allocations")
    @PreAuthorize("hasAnyRole('GSEManager', 'GroundSupervisor')")
    public ResponseEntity<ApiResponse<List<EquipmentAllocationResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success("Active allocations fetched",
                gseService.getAllActiveAllocations()));
    }

    @PatchMapping("/api/allocations/{id}/release")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<EquipmentAllocationResponse>> release(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Equipment released",
                gseService.release(id)));
    }

    // ─── Maintenance ────────────────────────────────────────────────────────────

    @PostMapping("/api/maintenance")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<EquipmentMaintenanceResponse>> reportFault(
            @Valid @RequestBody EquipmentMaintenanceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Maintenance reported",
                        gseService.reportMaintenance(request, userDetails.getUsername())));
    }

    @GetMapping("/api/maintenance")
    @PreAuthorize("hasAnyRole('GSEManager', 'Admin')")
    public ResponseEntity<ApiResponse<List<EquipmentMaintenanceResponse>>> getAllMaintenanceRecords() {
        return ResponseEntity.ok(ApiResponse.success("Maintenance records fetched",
                gseService.getAllMaintenance()));
    }

    @PatchMapping("/api/maintenance/{id}/resolve")
    @PreAuthorize("hasRole('GSEManager')")
    public ResponseEntity<ApiResponse<EquipmentMaintenanceResponse>> resolve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Equipment returned to service",
                gseService.resolveMaintenence(id)));
    }
}
