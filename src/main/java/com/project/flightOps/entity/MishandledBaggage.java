package com.project.flightOps.entity;

import com.project.flightOps.enums.MishandledStatus;
import com.project.flightOps.enums.MishandledType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mishandled_baggage")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MishandledBaggage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String mishandleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private String passengerName;

    @Column(nullable = false)
    private String bagTagNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MishandledType mishandleType;

    @Column(nullable = false)
    private LocalDateTime reportedDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MishandledStatus status;
}
