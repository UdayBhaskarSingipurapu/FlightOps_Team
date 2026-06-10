package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckInCounterRequest {

    @NotBlank(message = "Counter number is required")
    private String counterNumber;

    @NotBlank(message = "Terminal is required")
    private String terminal;

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    // Agent assigned at the counter (optional at creation)
    private String assignedAgentId;

    @NotNull(message = "Open time is required")
    private LocalDateTime openTime;

    private LocalDateTime closeTime;
}
