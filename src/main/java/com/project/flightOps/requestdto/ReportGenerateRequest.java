package com.project.flightOps.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportGenerateRequest {

    // Scope examples: "Airline:AI", "Shift:Morning", "Stand:14", "Period:2025-06"
    @NotBlank(message = "Scope is required (e.g. Airline:AI, Shift:Morning, Stand:14)")
    private String scope;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;
}
