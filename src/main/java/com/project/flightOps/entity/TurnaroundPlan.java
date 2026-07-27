package com.project.flightOps.entity;

import com.project.flightOps.enums.TurnaroundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;

@Entity
@Table(name = "turnaround_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnaroundPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String planId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false, unique = true)
    private Flight flight;

    private Integer targetTurnaroundMinutes;
    private Integer actualTurnaroundMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private User supervisor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurnaroundStatus status;

    @CreatedDate
    @Column(name = "plan_created_date")
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
    }
}
