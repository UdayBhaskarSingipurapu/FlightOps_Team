package com.project.flightOps.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ground_ops_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroundOpsReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String reportId;

    @Column(nullable = false)
    private String scope;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String metrics;

    @Column(nullable = false)
    private LocalDateTime generatedDate = LocalDateTime.now();
}
