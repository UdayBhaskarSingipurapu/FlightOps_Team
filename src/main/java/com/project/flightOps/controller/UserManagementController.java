package com.project.flightOps.controller;

import com.project.flightOps.requestdto.CreateUserRequest;
import com.project.flightOps.requestdto.UpdateUserRequest;
import com.project.flightOps.requestdto.UserStatusRequest;
import com.project.flightOps.responsedto.UserResponse;
import com.project.flightOps.service.UserManagementService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('Admin')")
@RequiredArgsConstructor
@Slf4j // 1. Added Lombok's logging annotation
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        // 2. Added context-rich logging statements
        log.info("Received request to create a new user with username/email: {}", request.getEmail());
        log.debug("Create user payload: {}", request);

        UserResponse response = userManagementService.createUser(request);

        log.info("Successfully created user with ID: {}", response.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        log.info("Received request to fetch all users");

        List<UserResponse> users = userManagementService.getAllUsers();

        log.info("Successfully fetched {} users", users.size());
        return ResponseEntity.ok(ApiResponse.success("Users fetched", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable String id) {
        log.info("Received request to fetch user with ID: {}", id);

        UserResponse response = userManagementService.getUserById(id);

        log.info("Successfully fetched user details for ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("User fetched", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Received request to update user with ID: {}", id);
        log.debug("Update user payload for ID {}: {}", id, request);

        UserResponse response = userManagementService.updateUser(id, request);

        log.info("Successfully updated user with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("User updated", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UserStatusRequest request) {
        log.info("Received request to update status for user ID: {}", id);
        log.debug("Status update payload for ID {}: {}", id, request);

        UserResponse response = userManagementService.updateStatus(id, request);

        log.info("Successfully updated status for user ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("User status updated", response));
    }
}