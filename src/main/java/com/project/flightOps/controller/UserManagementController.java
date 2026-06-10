package com.project.flightOps.controller;

import com.project.flightOps.requestdto.CreateUserRequest;
import com.project.flightOps.requestdto.UpdateUserRequest;
import com.project.flightOps.requestdto.UserStatusRequest;
import com.project.flightOps.responsedto.UserResponse;
import com.project.flightOps.service.UserManagementService;
import com.project.flightOps.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('Admin')")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created",
                        userManagementService.createUser(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Users fetched",
                userManagementService.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("User fetched",
                userManagementService.getUserById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated",
                userManagementService.updateUser(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User status updated",
                userManagementService.updateStatus(id, request)));
    }
}
