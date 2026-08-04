package com.project.flightOps.service;

import com.project.flightOps.entity.User;
import com.project.flightOps.enums.Role;
import com.project.flightOps.enums.UserStatus;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.CreateUserRequest;
import com.project.flightOps.requestdto.UpdateUserRequest;
import com.project.flightOps.requestdto.UserStatusRequest;
import com.project.flightOps.responsedto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userManagementService;

    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private UserStatusRequest userStatusRequest;
    private User existingUser;

    private final String userId = "USR-100";
    private final String email = "john.doe@airport.com";
    private final String encodedPassword = "ENCODED_PASSWORD";

    @BeforeEach
    void setUp() {
        createUserRequest = new CreateUserRequest();
        createUserRequest.setName("John Doe");
        createUserRequest.setEmail(email);
        createUserRequest.setPassword("plainPassword123");
        createUserRequest.setRole(Role.RampOfficer);
        createUserRequest.setPhone("9999999999");
        createUserRequest.setAirportId("APT-1");

        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setName("John Updated");
        updateUserRequest.setEmail(email);
        updateUserRequest.setRole(Role.GroundSupervisor);
        updateUserRequest.setPhone("8888888888");
        updateUserRequest.setAirportId("APT-2");

        userStatusRequest = new UserStatusRequest();
        userStatusRequest.setStatus(UserStatus.Inactive);

        existingUser = User.builder()
                .userId(userId)
                .name("John Doe")
                .email(email)
                .password(encodedPassword)
                .role(Role.RampOfficer)
                .phone("9999999999")
                .airportId("APT-1")
                .status(UserStatus.Active)
                .build();
    }

    // --- createUser Tests ---

    @Test
    void createUser_Success() {
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode("plainPassword123")).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        UserResponse response = userManagementService.createUser(createUserRequest);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("John Doe", response.getName());
        assertEquals(email, response.getEmail());
        assertEquals(Role.RampOfficer, response.getRole());
        assertEquals(UserStatus.Active, response.getStatus());
        verify(passwordEncoder).encode("plainPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ThrowsConflictException_WhenEmailAlreadyRegistered() {
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(ConflictException.class, () -> userManagementService.createUser(createUserRequest));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void createUser_NewUserAlwaysDefaultsToActiveStatus() {
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userManagementService.createUser(createUserRequest);

        assertEquals(UserStatus.Active, response.getStatus());
    }

}
