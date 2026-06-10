package com.project.flightOps.requestdto;

import com.project.flightOps.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HandlingStatusRequest {

    @NotNull(message = "Status is required")
    private RequestStatus status;
}
