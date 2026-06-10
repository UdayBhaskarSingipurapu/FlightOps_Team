package com.project.flightOps.responsedto;

import com.project.flightOps.enums.MaintenanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EquipmentMaintenanceResponse {
    private String maintenanceId;
    private String equipmentId;
    private String registrationNumber;
    private String issue;
    private String reportedById;
    private String reportedByName;
    private LocalDateTime reportedDate;
    private LocalDateTime expectedReturnDate;
    private MaintenanceStatus status;
}
