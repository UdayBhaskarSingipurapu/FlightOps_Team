package com.project.flightOps.responsedto;

import com.project.flightOps.enums.FlightStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FlightResponse {
    private String flightId;
    private String airlineCode;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDateTime scheduledArrival;
    private LocalDateTime scheduledDeparture;
    private String aircraftType;
    private Integer paxCapacity;
    private String stand;
    private FlightStatus status;
}
