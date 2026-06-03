package com.project.flightOps.entity;

import com.project.flightOps.enums.FlightStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String flightId;

    @Column(nullable = false)
    private String airlineCode;

    @Column(nullable = false)
    private String flightNumber;

    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private LocalDateTime scheduledArrival;

    @Column(nullable = false)
    private LocalDateTime scheduledDeparture;

    @Column(nullable = false)
    private String aircraftType;

    private Integer paxCapacity;
    private String stand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightStatus status;
}
