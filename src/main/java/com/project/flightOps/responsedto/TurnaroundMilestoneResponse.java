package com.project.flightOps.responsedto;

import com.project.flightOps.enums.MilestoneStatus;
import com.project.flightOps.enums.MilestoneType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TurnaroundMilestoneResponse {
    private String milestoneId;
    private String planId;
    private String flightId;
    private String flightNumber;
    private MilestoneType milestoneType;
    private LocalDateTime plannedTime;
    private LocalDateTime actualTime;
    private String completedById;
    private String completedByName;
    private MilestoneStatus status;
    private boolean isDelayed;
    private Long delayMinutes; // null if not delayed
}
