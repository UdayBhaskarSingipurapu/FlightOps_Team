package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class BaggageCountRequest {

    @NotNull(message = "Bags processed count is required")
    @PositiveOrZero(message = "Bags processed cannot be negative")
    private Integer totalBagsProcessed;
}
