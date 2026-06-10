package com.project.flightOps.requestdto;

import com.project.flightOps.enums.GateStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GateStatusRequest {

    @NotNull(message = "Status is required")
    private GateStatus status;
}
