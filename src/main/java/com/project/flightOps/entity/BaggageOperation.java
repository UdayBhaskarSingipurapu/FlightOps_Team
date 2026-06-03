package com.project.flightOps.entity;

import com.project.flightOps.enums.Direction;
import com.project.flightOps.enums.OperationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "baggage_operations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaggageOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String operationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false)
    private Integer totalBagsExpected;

    @Column(nullable = false)
    private Integer totalBagsProcessed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationStatus status;
}
