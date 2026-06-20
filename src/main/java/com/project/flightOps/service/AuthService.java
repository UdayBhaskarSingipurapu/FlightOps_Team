package com.project.flightOps.service;

import com.project.flightOps.entity.User;
import com.project.flightOps.enums.UserStatus;
import com.project.flightOps.exception.UserNotFoundException;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.LoginRequest;
import com.project.flightOps.requestdto.RefreshTokenRequest;
import com.project.flightOps.requestdto.RegisterRequest;
import com.project.flightOps.responsedto.AuthResponse;
import com.project.flightOps.responsedto.RefreshTokenResponse;
import com.project.flightOps.util.UserPrincipal;
import lombok.extern.slf4j.Slf4j; // Added Lombok Slf4j import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j // Added annotation to automatically inject the 'log' object
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Transactional
    public String register(RegisterRequest request) {
        log.info("Attempting to register a new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Account with email {} already exists.", request.getEmail());
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .airportId(request.getAirportId())
                .phone(request.getPhone())
                .status(UserStatus.Active) // New registrations are active by default
                .build();

        userRepository.save(user);

        log.info("User registered successfully. Assigned User ID: {}", user.getUserId());
        return "User registered successfully with ID: " + user.getUserId();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Authentication request received for user: {}", request.getEmail());

        try {
            // 1. Authenticate user credentials via Spring Security context managers
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            log.debug("Spring Security authentication successful for user: {}", request.getEmail());
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {} - Reason: {}", request.getEmail(), e.getMessage());
            throw e; // Re-throw to be handled by global exception handler
        }

        // 2. Load our concrete database user record
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
            log.error("Database mismatch: User authenticated but records not found for email: {}", request.getEmail());
            return new UsernameNotFoundException("User records not found.");
        });

        // 3. Adapt domain entity into UserDetails Principal wrapper context
        UserPrincipal userPrincipal = new UserPrincipal(user);

        // 4. Run token generation routines using your secure configurations
        log.debug("Generating secure tokens for user ID: {}", user.getUserId());
        String accessToken = jwtService.generateAccessToken(userPrincipal, user.getRole(), user.getUserId());
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        log.info("User {} successfully logged in.", request.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Token refresh operation initiated.");
        String refreshToken = request.getRefreshToken();

        // 1. Extract identifier from the incoming token
        String email = jwtService.extractEmail(refreshToken);

        if (email == null) {
            log.warn("Token refresh rejected: Unable to extract email from payload.");
            throw new IllegalArgumentException("Invalid refresh token payload.");
        }

        log.debug("Extract email from refresh token: {}", email);

        // 2. Load the actual database user
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            log.error("Token refresh failed: User associated with email {} no longer exists in DB.", email);
            return new UserNotFoundException("User associated with this token no longer exists.");
        });

        UserPrincipal userPrincipal = new UserPrincipal(user);

        // 3. Verify token validity and expiration state
        if (!jwtService.isTokenValid(refreshToken, userPrincipal)) {
            log.warn("Token refresh rejected: Refresh token for {} is expired or corrupted.", email);
            throw new IllegalArgumentException("Refresh token has expired or is corrupt. Please log in again.");
        }

        // 4. Issue a clean new Access Token and a rolling Refresh Token
        log.debug("Generating rolling tokens for user: {}", email);
        String newAccessToken = jwtService.generateAccessToken(userPrincipal, user.getRole(), user.getUserId());
//        String newRefreshToken = jwtService.generateRefreshToken(userPrincipal);

        log.info("Tokens successfully refreshed for user: {}", email);
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }
}