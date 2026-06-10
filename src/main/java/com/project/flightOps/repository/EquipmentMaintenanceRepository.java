package com.project.flightOps.repository;

import com.project.flightOps.entity.EquipmentMaintenance;
import com.project.flightOps.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentMaintenanceRepository extends JpaRepository<EquipmentMaintenance, String> {

    List<EquipmentMaintenance> findByEquipment_EquipmentIdOrderByReportedDateDesc(String equipmentId);

    List<EquipmentMaintenance> findByStatusOrderByReportedDateDesc(MaintenanceStatus status);

    List<EquipmentMaintenance> findAllByOrderByReportedDateDesc();
}
