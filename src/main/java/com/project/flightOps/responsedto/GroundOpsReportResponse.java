package com.project.flightOps.responsedto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GroundOpsReportResponse {
    private String reportId;
    private String scope;
    private String metrics;         // stored as JSON string in DB
    private LocalDateTime generatedDate;
}
