package com.project.flightOps.requestdto;

import com.project.flightOps.enums.FlightStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FlightStatusRequest {

    @NotNull(message = "Status is required")
    private FlightStatus status;
}
