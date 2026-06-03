package com.project.flightOps.entity;

import com.project.flightOps.enums.MaintenanceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_maintenance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentMaintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String maintenanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private GroundEquipment equipment;

    @Column(nullable = false)
    private String issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private User reportedBy;

    @Column(nullable = false)
    private LocalDateTime reportedDate = LocalDateTime.now();

    private LocalDateTime expectedReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status;
}
