package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TurnaroundPlanRequest {

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    @NotNull(message = "Target turnaround minutes is required")
    @Positive(message = "Target minutes must be positive")
    private Integer targetTurnaroundMinutes;
}
