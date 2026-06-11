package com.project.flightOps.controller;

import com.project.flightOps.requestdto.NotificationRequest;
import com.project.flightOps.responsedto.NotificationResponse;
import com.project.flightOps.service.NotificationService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j // Enables the 'log' object automatically via Lombok
public class NotificationController {

    private final NotificationService notificationService;

    // Internal trigger — Admin only (other services call this via REST internally)
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody NotificationRequest request) {
        log.info("Received request to create an internal notification");
        log.debug("Notification creation payload: {}", request);

        NotificationResponse response = notificationService.create(request);

        log.info("Successfully created internal notification");
        return ResponseEntity.ok(ApiResponse.success("Notification created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = extractUserId(userDetails);
        log.info("Fetching all notifications for user ID: {}", userId);

        List<NotificationResponse> response = notificationService.getAllForUser(userId);

        log.info("Successfully fetched {} notifications for user ID: {}", response.size(), userId);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", response));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnread(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = extractUserId(userDetails);
        log.info("Fetching unread notifications for user ID: {}", userId);

        List<NotificationResponse> response = notificationService.getUnreadForUser(userId);

        log.info("Successfully fetched {} unread notifications for user ID: {}", response.size(), userId);
        return ResponseEntity.ok(ApiResponse.success("Unread notifications fetched", response));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable String id) {
        log.info("Received request to mark notification ID: {} as read", id);
        NotificationResponse response = notificationService.markAsRead(id);
        log.info("Successfully marked notification ID: {} as read", id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", response));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = extractUserId(userDetails);
        log.info("Received request to mark all notifications as read for user ID: {}", userId);

        notificationService.markAllAsRead(userId);

        log.info("Successfully marked all notifications as read for user ID: {}", userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> dismiss(@PathVariable String id) {
        log.info("Received request to dismiss notification ID: {}", id);
        notificationService.dismiss(id);
        log.info("Successfully dismissed notification ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Notification dismissed", null));
    }

    // Assumption: UserDetails username is the userId (as set by your existing JWT implementation)
    private String extractUserId(UserDetails userDetails) {
        return userDetails.getUsername();
    }
}