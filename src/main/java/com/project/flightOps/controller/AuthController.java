package com.project.flightOps.controller;

import com.project.flightOps.requestdto.LoginRequest;
import com.project.flightOps.requestdto.RefreshTokenRequest;
import com.project.flightOps.requestdto.RegisterRequest;
import com.project.flightOps.responsedto.AuthResponse;
import com.project.flightOps.responsedto.RefreshTokenResponse;
import com.project.flightOps.service.AuthService;
import com.project.flightOps.util.ApiResponse;
import lombok.extern.slf4j.Slf4j; // 1. Added Slf4j import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j // 2. Added annotation to auto-generate the 'log' object
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody RegisterRequest request) {
        // Notice we only call getUsername(), NEVER a getPassword() method into the logs!
        log.info("REST request to register a new user account. Username: {}", request.getName());

        String result = authService.register(request);

        log.info("User registration completed successfully for username: {}", request.getName());
        ApiResponse<?> response = ApiResponse.success("registration successful");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody LoginRequest request) {
        // By not calling any getter methods, the error disappears instantly!
        log.info("REST request to authenticate a user login session.");

        AuthResponse response = authService.login(request);
        log.debug("Authentication successful. JWT Token issued.");
        ApiResponse<?> loginResponse = ApiResponse.success("login successful", response);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        // We log the rotation request using a snippet or placeholder indicator instead of the full key
        log.info("REST request to rotate and refresh expired authentication JWT token.");

        RefreshTokenResponse response = authService.refreshToken(request);

        log.debug("Refresh token rotation executed successfully.");
        ApiResponse<?> refreshResponse = ApiResponse.success("Refresh token generated successfully", response);
        return ResponseEntity.ok(response);
    }
}