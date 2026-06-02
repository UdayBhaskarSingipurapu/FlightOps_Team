package com.project.flightOps.repository;

import com.project.flightOps.entity.User;
import com.project.flightOps.enums.Roles;
import com.project.flightOps.responsedto.UserResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<UserResponseDTO> findByRole(Roles role);
    List<User> findByFullNameContainingIgnoreCase(String name);
}
