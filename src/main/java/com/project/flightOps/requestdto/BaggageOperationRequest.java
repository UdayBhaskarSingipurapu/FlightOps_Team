package com.project.flightOps.requestdto;

import com.project.flightOps.enums.Direction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaggageOperationRequest {

    @NotBlank(message = "Flight ID is required")
    private String flightId;

    @NotNull(message = "Direction is required (Inbound or Outbound)")
    private Direction direction;

    @NotNull(message = "Total bags expected is required")
    @Positive(message = "Expected bags must be a positive number")
    private Integer totalBagsExpected;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
}
