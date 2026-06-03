package com.project.flightOps.entity;

import com.project.flightOps.enums.MilestoneStatus;
import com.project.flightOps.enums.MilestoneType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "turnaround_milestones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnaroundMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String milestoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private TurnaroundPlan turnaroundPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneType milestoneType;

    @Column(nullable = false)
    private LocalDateTime plannedTime;

    private LocalDateTime actualTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_id")
    private User completedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneStatus status;
}
