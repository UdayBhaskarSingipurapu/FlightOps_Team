package com.project.flightOps.entity;

import com.project.flightOps.enums.CounterStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "check_in_counters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInCounter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String counterId;

    @Column(nullable = false)
    private String counterNumber;

    @Column(nullable = false)
    private String terminal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    private LocalDateTime openTime;
    private LocalDateTime closeTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounterStatus status;
}
