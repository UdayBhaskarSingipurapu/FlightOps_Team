package com.project.flightOps.repository;

import com.project.flightOps.entity.EquipmentAllocation;
import com.project.flightOps.entity.GroundEquipment;
import com.project.flightOps.enums.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentAllocationRepository extends JpaRepository<EquipmentAllocation, String> {

    List<EquipmentAllocation> findByFlight_FlightId(String flightId);

    List<EquipmentAllocation> findByStatusOrderByAllocationTimeDesc(AllocationStatus status);

    boolean existsByEquipmentAndStatus(GroundEquipment equipment, AllocationStatus status);

    List<EquipmentAllocation> findByEquipment(GroundEquipment equipment);
}
