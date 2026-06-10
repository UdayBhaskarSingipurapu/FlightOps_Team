package com.project.flightOps.responsedto;

import com.project.flightOps.enums.GateStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BoardingGateResponse {
    private String gateId;
    private String gateNumber;
    private String terminal;
    private String flightId;
    private String flightNumber;
    private String assignedAgentId;
    private String assignedAgentName;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private GateStatus status;
}
