package com.project.flightOps.requestdto;

import com.project.flightOps.enums.AssistanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpecialAssistanceRequest {

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Passenger name is required")
    private String passengerName;

    @NotNull(message = "Assistance type is required")
    private AssistanceType assistanceType;
}
