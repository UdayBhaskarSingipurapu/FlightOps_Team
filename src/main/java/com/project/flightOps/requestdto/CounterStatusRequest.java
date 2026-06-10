package com.project.flightOps.requestdto;

import com.project.flightOps.enums.CounterStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CounterStatusRequest {

    @NotNull(message = "Status is required")
    private CounterStatus status;
}
