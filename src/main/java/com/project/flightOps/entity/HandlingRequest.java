package com.project.flightOps.entity;

import com.project.flightOps.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;

@Entity
@Table(name = "handling_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandlingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private String airlineId;

    @Column(nullable = false)
    private String serviceTypes;

    private String specialRequirements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @CreatedDate
    @Column(name = "request_created_date")
    private LocalDate createdAt;

    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDate.now();
    }
}