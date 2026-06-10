package com.project.flightOps.responsedto;

import com.project.flightOps.enums.Direction;
import com.project.flightOps.enums.OperationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BaggageOperationResponse {
    private String operationId;
    private String flightId;
    private String flightNumber;
    private Direction direction;
    private Integer totalBagsExpected;
    private Integer totalBagsProcessed;
    private Integer discrepancy;     // computed: expected - processed
    private String operatorId;
    private String operatorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private OperationStatus status;
}
