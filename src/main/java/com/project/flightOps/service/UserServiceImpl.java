package com.project.flightOps.service;


import com.project.flightOps.entity.User;
import com.project.flightOps.enums.Roles;
import com.project.flightOps.exception.UserAlreadyExistsException;
import com.project.flightOps.exception.UserNotFoundException;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.UserRequestDTO;
import com.project.flightOps.responsedto.UserResponseDTO;
import com.project.flightOps.security.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @Override
    public User addUser(UserRequestDTO userRequestDTO) {

        Optional<User> existingUser = userRepo.findByEmail(userRequestDTO.getEmail());
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("User with email " + userRequestDTO.getEmail() + " already exists.");
        }

        User user = new User();
        user.setFullName(userRequestDTO.getFullName());
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(encoder.encode(userRequestDTO.getPassword()));
        user.setRole(userRequestDTO.getRole());
        user.setAddress(userRequestDTO.getAddress());
        user.setPhoneNo(userRequestDTO.getPhoneNo());
        return userRepo.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public User getUserById(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    @Override
    public List<UserResponseDTO> getUsersByRole(Roles role) {
        List<UserResponseDTO> users = userRepo.findByRole(role);
        if (users.isEmpty()) {
            throw new UserNotFoundException("No users found with role: " + role);
        }
        return users;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Loading user by username: " + username);
        User user = userRepo.findByEmail(username)
                .orElseThrow(() -> {
                    System.out.println("User not found: " + username);
                    return new UsernameNotFoundException("User not found with email: " + username);
                });

        System.out.println("User found: " + user.getEmail());
        System.out.println("Password hash: " + user.getPassword());
        System.out.println("Password hash length: " + user.getPassword().length());

        if (user.getRole() == null) {
            System.out.println("User role is null, setting default role to USER");
            user.setRole(com.project.flightOps.enums.Roles.USER);
        }

        return new CustomUserDetails(user); // ✅ return your custom class
    }

    @Override
    public List<UserResponseDTO> getAllMembers() {

        return userRepo.findAll().stream().map(user -> new UserResponseDTO(user.getUserID(), user.getEmail(), user.getFullName(), user.getRole(), user.getPhoneNo(), user.getAddress())).collect(Collectors.toList());
    }

    @Override
    public Optional<UserResponseDTO> updateMember(int id, UserRequestDTO updatedUserDTO) {
        return userRepo.findById((long) id).map(user -> {
            user.setFullName(updatedUserDTO.getFullName());
            user.setAddress(updatedUserDTO.getAddress());
            user.setEmail(updatedUserDTO.getEmail());
            user.setPhoneNo(updatedUserDTO.getPhoneNo());


            User updatedUser = userRepo.save(user);
            return new UserResponseDTO(updatedUser.getUserID(), updatedUser.getEmail(),updatedUser.getFullName(),updatedUser.getRole(),updatedUser.getPhoneNo(),updatedUser.getAddress());
        });
    }
    @Override
    public void deleteMember(Long id) {
        Optional<User> user = userRepo.findById(id);
        userRepo.deleteById(user.get().getUserID());
    }

    @Override
    public List<UserResponseDTO> searchByName(String name) {
        return userRepo.findByFullNameContainingIgnoreCase(name).stream()
                .map(user -> new UserResponseDTO(
                        user.getUserId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole(),
                        user.getPhoneNo(),
                        user.getAddress()
                ))
                .toList();
    }

    @Override
    public List<UserResponseDTO> searchByEmail(String email) {
        return userRepo.findByFullNameContainingIgnoreCase(email).stream()
                .map(user -> new UserResponseDTO(
                        user.getUserId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getRole(),
                        user.getPhoneNo(),
                        user.getAddress()
                ))
                .toList();
    }





}
