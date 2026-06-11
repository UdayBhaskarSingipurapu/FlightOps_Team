package com.project.flightOps.service;

import com.project.flightOps.entity.User;
import com.project.flightOps.enums.UserStatus;
import com.project.flightOps.exception.ConflictException;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.CreateUserRequest;
import com.project.flightOps.requestdto.UpdateUserRequest;
import com.project.flightOps.requestdto.UserStatusRequest;
import com.project.flightOps.responsedto.UserResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Attempting to create user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User creation failed. Email already registered: {}", request.getEmail());
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phone(request.getPhone())
                .airportId(request.getAirportId())
                .status(UserStatus.Active)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User successfully created with ID: {}", savedUser.getUserId());
        return toResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users from the database");
        List<User> users = userRepository.findAll();
        log.info("Retrieved {} users", users.size());
        return users.stream().map(this::toResponse).toList();
    }

    public UserResponse getUserById(String userId) {
        log.debug("Fetching user with ID: {}", userId);
        return toResponse(findById(userId));
    }

    @Transactional
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        log.info("Attempting to update user with ID: {}", userId);
        User user = findById(userId);

        // Check email uniqueness only if it's being changed
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            log.warn("Update failed for user ID {}. Email already in use: {}", userId, request.getEmail());
            throw new ConflictException("Email already in use: " + request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setAirportId(request.getAirportId());

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated profile for user ID: {}", userId);
        return toResponse(updatedUser);
    }

    @Transactional
    public UserResponse updateStatus(String userId, UserStatusRequest request) {
        log.info("Attempting to update status for user ID: {} to {}", userId, request.getStatus());
        User user = findById(userId);

        user.setStatus(request.getStatus());
        User updatedUser = userRepository.save(user);
        log.info("Successfully updated status to {} for user ID: {}", request.getStatus(), userId);
        return toResponse(updatedUser);
    }

    private User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Resource lookup failed. User not found with ID: {}", id);
                    return new ResourceNotFoundException("User not found: " + id);
                });
    }

    public UserResponse toResponse(User u) {
        return UserResponse.builder()
                .userId(u.getUserId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .phone(u.getPhone())
                .airportId(u.getAirportId())
                .status(u.getStatus())
                .build();
    }
}