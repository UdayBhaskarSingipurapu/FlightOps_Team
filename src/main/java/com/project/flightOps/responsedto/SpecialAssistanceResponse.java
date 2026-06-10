package com.project.flightOps.responsedto;

import com.project.flightOps.enums.AssistanceStatus;
import com.project.flightOps.enums.AssistanceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpecialAssistanceResponse {
    private String assistanceId;
    private String flightId;
    private String flightNumber;
    private String passengerName;
    private AssistanceType assistanceType;
    private String assignedAgentId;
    private String assignedAgentName;
    private AssistanceStatus status;
}
