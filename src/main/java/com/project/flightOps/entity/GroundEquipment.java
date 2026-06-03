package com.project.flightOps.entity;

import com.project.flightOps.enums.EquipmentStatus;
import com.project.flightOps.enums.EquipmentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ground_equipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroundEquipment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String equipmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentType type;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    private String currentLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus status;
}
