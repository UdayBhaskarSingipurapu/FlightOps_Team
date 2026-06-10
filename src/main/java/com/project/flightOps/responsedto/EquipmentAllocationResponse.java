package com.project.flightOps.responsedto;

import com.project.flightOps.enums.AllocationStatus;
import com.project.flightOps.enums.EquipmentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EquipmentAllocationResponse {
    private String allocationId;
    private String equipmentId;
    private String registrationNumber;
    private EquipmentType equipmentType;
    private String flightId;
    private String flightNumber;
    private String allocatedById;
    private String allocatedByName;
    private LocalDateTime allocationTime;
    private LocalDateTime releaseTime;
    private AllocationStatus status;
}
