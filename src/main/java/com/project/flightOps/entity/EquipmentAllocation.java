package com.project.flightOps.entity;

import com.project.flightOps.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_allocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String allocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private GroundEquipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_by_id", nullable = false)
    private User allocatedBy;

    @Column(nullable = false)
    private LocalDateTime allocationTime = LocalDateTime.now();

    private LocalDateTime releaseTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status;
}