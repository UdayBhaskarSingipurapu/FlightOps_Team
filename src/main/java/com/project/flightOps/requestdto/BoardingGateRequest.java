package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BoardingGateRequest {

    @NotBlank(message = "Gate number is required")
    private String gateNumber;

    @NotBlank(message = "Terminal is required")
    private String terminal;

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    private String assignedAgentId;

    @NotNull(message = "Open time is required")
    private LocalDateTime openTime;

    private LocalDateTime closeTime;
}
