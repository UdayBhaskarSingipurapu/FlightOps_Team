package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EquipmentAllocationRequest {

    @NotBlank(message = "Equipment ID is required")
    private String equipmentId;

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    @NotNull(message = "Allocation time is required")
    private LocalDateTime allocationTime;

    private LocalDateTime releaseTime;
}
