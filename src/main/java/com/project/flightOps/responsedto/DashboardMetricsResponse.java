package com.project.flightOps.responsedto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DashboardMetricsResponse {
    private LocalDate date;
    private int totalFlightsHandled;
    private int onTimeTurnarounds;
    private int delayedTurnarounds;
    private double onTimeRatePercent;
    private double avgTurnaroundMinutes;
    private int totalEquipment;
    private int allocatedEquipment;
    private double gseUtilisationPercent;
    private int totalBaggageOps;
    private int discrepancyOps;
    private double baggageDiscrepancyRatePercent;
    private int slaBreachCount;
    private int mishandledBagsReported;
    private int openAssistanceRequests;
}
