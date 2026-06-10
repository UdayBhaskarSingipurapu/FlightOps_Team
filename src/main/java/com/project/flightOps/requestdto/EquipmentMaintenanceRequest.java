package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EquipmentMaintenanceRequest {

    @NotBlank(message = "Equipment ID is required")
    private String equipmentId;

    @NotBlank(message = "Issue description is required")
    private String issue;

    private LocalDateTime expectedReturnDate;
}
