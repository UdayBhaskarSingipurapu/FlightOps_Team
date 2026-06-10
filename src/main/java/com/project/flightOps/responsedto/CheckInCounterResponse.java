package com.project.flightOps.responsedto;

import com.project.flightOps.enums.CounterStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CheckInCounterResponse {
    private String counterId;
    private String counterNumber;
    private String terminal;
    private String flightId;
    private String flightNumber;
    private String assignedAgentId;
    private String assignedAgentName;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private CounterStatus status;
}
