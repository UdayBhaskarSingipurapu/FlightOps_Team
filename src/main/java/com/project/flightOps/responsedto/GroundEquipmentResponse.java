package com.project.flightOps.responsedto;

import com.project.flightOps.enums.EquipmentStatus;
import com.project.flightOps.enums.EquipmentType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroundEquipmentResponse {
    private String equipmentId;
    private EquipmentType type;
    private String registrationNumber;
    private String currentLocation;
    private EquipmentStatus status;
}
