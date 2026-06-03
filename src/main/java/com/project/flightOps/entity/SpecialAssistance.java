package com.project.flightOps.entity;

import com.project.flightOps.enums.AssistanceStatus;
import com.project.flightOps.enums.AssistanceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "special_assistance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialAssistance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String assistanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private String passengerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssistanceType assistanceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssistanceStatus status;
}
