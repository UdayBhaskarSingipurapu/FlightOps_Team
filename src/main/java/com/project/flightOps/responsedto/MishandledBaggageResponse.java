package com.project.flightOps.responsedto;

import com.project.flightOps.enums.MishandledStatus;
import com.project.flightOps.enums.MishandledType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MishandledBaggageResponse {
    private String mishandleId;
    private String flightId;
    private String flightNumber;
    private String passengerName;
    private String bagTagNumber;
    private MishandledType mishandleType;
    private LocalDateTime reportedDate;
    private MishandledStatus status;
}
