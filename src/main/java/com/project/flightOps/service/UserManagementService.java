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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
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
        return toResponse(userRepository.save(user));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse getUserById(String userId) {
        return toResponse(findById(userId));
    }

    @Transactional
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = findById(userId);

        // Check email uniqueness only if it's being changed
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already in use: " + request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setAirportId(request.getAirportId());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateStatus(String userId, UserStatusRequest request) {
        User user = findById(userId);
        user.setStatus(request.getStatus());
        return toResponse(userRepository.save(user));
    }

    private User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
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
