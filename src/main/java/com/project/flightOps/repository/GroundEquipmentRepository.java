package com.project.flightOps.repository;

import com.project.flightOps.entity.GroundEquipment;
import com.project.flightOps.enums.EquipmentStatus;
import com.project.flightOps.enums.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroundEquipmentRepository extends JpaRepository<GroundEquipment, String> {

    List<GroundEquipment> findByStatus(EquipmentStatus status);

    List<GroundEquipment> findByTypeAndStatus(EquipmentType type, EquipmentStatus status);

    Optional<GroundEquipment> findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);
}
