package com.project.flightOps.responsedto;

import com.project.flightOps.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HandlingRequestResponse {
    private String requestId;
    private String flightId;
    private String flightNumber;
    private String airlineId;
    private String serviceTypes;
    private String specialRequirements;
    private String requestedById;
    private String requestedByName;
    private RequestStatus status;
}
