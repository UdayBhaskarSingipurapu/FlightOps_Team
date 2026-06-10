package com.project.flightOps.controller;

import com.project.flightOps.requestdto.MilestoneCompleteRequest;
import com.project.flightOps.requestdto.TurnaroundPlanRequest;
import com.project.flightOps.responsedto.TurnaroundMilestoneResponse;
import com.project.flightOps.responsedto.TurnaroundPlanResponse;
import com.project.flightOps.service.TurnaroundService;
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
public class TurnaroundController {

    private final TurnaroundService turnaroundService;

    // ─── Turnaround Plans ───────────────────────────────────────────────────────

    @PostMapping("/api/turnarounds")
    @PreAuthorize("hasRole('GroundSupervisor')")
    public ResponseEntity<ApiResponse<TurnaroundPlanResponse>> createPlan(
            @Valid @RequestBody TurnaroundPlanRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Turnaround plan created",
                        turnaroundService.createPlan(request, userDetails.getUsername())));
    }

    @GetMapping("/api/turnarounds")
    @PreAuthorize("hasAnyRole('GroundSupervisor', 'RampOfficer', 'Admin', 'AirlineCoordinator')")
    public ResponseEntity<ApiResponse<List<TurnaroundPlanResponse>>> getAll(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<TurnaroundPlanResponse> result = activeOnly
                ? turnaroundService.getActive()
                : turnaroundService.getAll();
        return ResponseEntity.ok(ApiResponse.success("Turnaround plans fetched", result));
    }

    @GetMapping("/api/turnarounds/{id}")
    public ResponseEntity<ApiResponse<TurnaroundPlanResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Turnaround plan fetched",
                turnaroundService.getById(id)));
    }

    @GetMapping("/api/turnarounds/flight/{flightId}")
    public ResponseEntity<ApiResponse<TurnaroundPlanResponse>> getByFlight(
            @PathVariable String flightId) {
        return ResponseEntity.ok(ApiResponse.success("Turnaround plan fetched",
                turnaroundService.getByFlight(flightId)));
    }

    @PatchMapping("/api/turnarounds/{id}/status")
    @PreAuthorize("hasRole('GroundSupervisor')")
    public ResponseEntity<ApiResponse<TurnaroundPlanResponse>> completePlan(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Turnaround completed",
                turnaroundService.completePlan(id)));
    }

    // ─── Milestones ─────────────────────────────────────────────────────────────

    @GetMapping("/api/milestones/turnaround/{planId}")
    public ResponseEntity<ApiResponse<List<TurnaroundMilestoneResponse>>> getMilestones(
            @PathVariable String planId) {
        return ResponseEntity.ok(ApiResponse.success("Milestones fetched",
                turnaroundService.getMilestonesByPlan(planId)));
    }

    @PatchMapping("/api/milestones/{id}/complete")
    @PreAuthorize("hasRole('RampOfficer')")
    public ResponseEntity<ApiResponse<TurnaroundMilestoneResponse>> completeMilestone(
            @PathVariable String id,
            @Valid @RequestBody MilestoneCompleteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Milestone completed",
                turnaroundService.completeMilestone(id, request, userDetails.getUsername())));
    }

    @GetMapping("/api/milestones/delayed")
    @PreAuthorize("hasRole('GroundSupervisor')")
    public ResponseEntity<ApiResponse<List<TurnaroundMilestoneResponse>>> getDelayed() {
        return ResponseEntity.ok(ApiResponse.success("Delayed milestones fetched",
                turnaroundService.getDelayedMilestones()));
    }

    @GetMapping("/api/milestones/{id}")
    public ResponseEntity<ApiResponse<TurnaroundMilestoneResponse>> getMilestoneById(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Milestone fetched",
                turnaroundService.getMilestoneById(id)));
    }
}
