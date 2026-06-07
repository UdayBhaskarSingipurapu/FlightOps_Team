package com.project.flightOps.controller;

import com.project.flightOps.requestdto.NotificationRequest;
import com.project.flightOps.responsedto.NotificationResponse;
import com.project.flightOps.service.NotificationService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Internal trigger — Admin only (other services call this via REST internally)
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Notification created",
                notificationService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched",
                notificationService.getAllForUser(userId)));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnread(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Unread notifications fetched",
                notificationService.getUnreadForUser(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Marked as read",
                notificationService.markAsRead(id)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(extractUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> dismiss(@PathVariable String id) {
        notificationService.dismiss(id);
        return ResponseEntity.ok(ApiResponse.success("Notification dismissed", null));
    }

    // Assumption: UserDetails username is the userId (as set by your existing JWT implementation)
    private String extractUserId(UserDetails userDetails) {
        return userDetails.getUsername();
    }
}
