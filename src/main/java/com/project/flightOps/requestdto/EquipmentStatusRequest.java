package com.project.flightOps.requestdto;

import com.project.flightOps.enums.EquipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EquipmentStatusRequest {

    @NotNull(message = "Status is required")
    private EquipmentStatus status;
}
