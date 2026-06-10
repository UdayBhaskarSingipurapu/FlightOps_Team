package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FlightRequest {

    @NotBlank(message = "Airline code is required")
    private String airlineCode;

    @NotBlank(message = "Flight number is required")
    private String flightNumber;

    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotNull(message = "Scheduled arrival is required")
    private LocalDateTime scheduledArrival;

    @NotNull(message = "Scheduled departure is required")
    private LocalDateTime scheduledDeparture;

    @NotBlank(message = "Aircraft type is required")
    private String aircraftType;

    @Positive(message = "Passenger capacity must be positive")
    private Integer paxCapacity;

    private String stand;
}
