package com.project.flightOps.requestdto;

import com.project.flightOps.enums.MishandledStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MishandledStatusRequest {

    @NotNull(message = "Status is required")
    private MishandledStatus status;
}
