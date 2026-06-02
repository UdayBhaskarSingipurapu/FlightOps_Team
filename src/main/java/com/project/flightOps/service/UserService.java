package com.project.flightOps.service;

import com.project.flightOps.entity.User;
import com.project.flightOps.enums.Roles;
import com.project.flightOps.requestdto.UserRequestDTO;
import com.project.flightOps.responsedto.UserResponseDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserService {
    @Transactional
    User addUser(UserRequestDTO user);
    @Transactional(readOnly = true)
    User getUserById(Long userId);

    List<UserResponseDTO> getUsersByRole(Roles role);

    List<UserResponseDTO> getAllMembers();

    Optional<UserResponseDTO> updateMember(int id, UserRequestDTO updatedMemberRequestDto);

    void deleteMember(Long id);


    List<UserResponseDTO> searchByName(String name);

    List<UserResponseDTO> searchByEmail(String email);
}
