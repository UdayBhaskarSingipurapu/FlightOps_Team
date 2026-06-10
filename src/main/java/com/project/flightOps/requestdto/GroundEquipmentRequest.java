package com.project.flightOps.requestdto;

import com.project.flightOps.enums.EquipmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroundEquipmentRequest {

    @NotNull(message = "Equipment type is required")
    private EquipmentType type;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    private String currentLocation;
}
