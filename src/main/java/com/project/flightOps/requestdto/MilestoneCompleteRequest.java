package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MilestoneCompleteRequest {

    @NotNull(message = "Actual completion time is required")
    private LocalDateTime actualTime;

    private String notes;
}
