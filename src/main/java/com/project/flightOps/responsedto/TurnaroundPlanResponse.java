package com.project.flightOps.responsedto;

import com.project.flightOps.enums.TurnaroundStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TurnaroundPlanResponse {
    private String planId;
    private String flightId;
    private String flightNumber;
    private String stand;
    private Integer targetTurnaroundMinutes;
    private Integer actualTurnaroundMinutes;
    private String supervisorId;
    private String supervisorName;
    private TurnaroundStatus status;
    private List<TurnaroundMilestoneResponse> milestones;
}
