package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HandlingRequestDto {

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    @NotBlank(message = "Airline ID is required")
    private String airlineId;

    // Comma-separated: "Ramp,Baggage,Fuel"
    @NotBlank(message = "Service types are required")
    private String serviceTypes;

    private String specialRequirements;
}
