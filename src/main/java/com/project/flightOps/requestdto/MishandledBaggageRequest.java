package com.project.flightOps.requestdto;

import com.project.flightOps.enums.MishandledType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MishandledBaggageRequest {

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    @NotBlank(message = "Passenger name is required")
    private String passengerName;

    @NotBlank(message = "Bag tag number is required")
    private String bagTagNumber;

    @NotNull(message = "Mishandle type is required")
    private MishandledType mishandleType;
}
