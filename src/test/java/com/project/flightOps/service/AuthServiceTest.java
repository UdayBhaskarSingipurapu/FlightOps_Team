package com.project.flightOps.service;

import com.project.flightOps.entity.User;
import com.project.flightOps.enums.Role;
import com.project.flightOps.enums.UserStatus;
import com.project.flightOps.exception.UserNotFoundException;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.LoginRequest;
import com.project.flightOps.requestdto.RefreshTokenRequest;
import com.project.flightOps.requestdto.RegisterRequest;
import com.project.flightOps.responsedto.AuthResponse;
import com.project.flightOps.responsedto.RefreshTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private User user;

    private final String userId = "USR-1";
    private final String email = "pilot@flightops.com";
    private final String rawPassword = "P@ssw0rd";
    private final String encodedPassword = "ENCODED_PASSWORD";
    private final String accessToken = "ACCESS_TOKEN";
    private final String refreshTokenValue = "REFRESH_TOKEN";

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Jane Pilot");
        registerRequest.setEmail(email);
        registerRequest.setPassword(rawPassword);
        registerRequest.setAirportId("JFK");
        registerRequest.setPhone("1234567890");
        registerRequest.setRole(Role.RampOfficer);

        loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(rawPassword);

        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken(refreshTokenValue);

        user = User.builder()
                .userId(userId)
                .name("Jane Pilot")
                .email(email)
                .password(encodedPassword)
                .role(Role.RampOfficer)
                .airportId("JFK")
                .phone("1234567890")
                .status(UserStatus.Active)
                .build();
    }

    // --- register Tests ---

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(userId);
            return saved;
        });

        String result = authService.register(registerRequest);

        assertNotNull(result);
        assertTrue(result.contains(userId));
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getEmail().equals(email)
                        && savedUser.getPassword().equals(encodedPassword)
                        && savedUser.getStatus() == UserStatus.Active
                        && savedUser.getRole() == Role.RampOfficer));
    }

    @Test
    void register_ThrowsIllegalArgumentException_WhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void register_ThrowsIllegalArgumentException_WhenEmailIsBlank() {
        registerRequest.setEmail("");
        when(userRepository.existsByEmail("")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));

        verify(userRepository, never()).save(any());
    }

}
