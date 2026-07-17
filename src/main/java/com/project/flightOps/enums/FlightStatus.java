package com.project.flightOps.enums;

import java.util.List;
import java.util.Map;

public enum FlightStatus {
    Scheduled,
    Arrived,
    Departed,
    Delayed,
    Diverted,
    Cancelled;

    private static final Map<FlightStatus, List<FlightStatus>> ALLOWED_TRANSITIONS = Map.of(
            Scheduled, List.of(Delayed, Arrived, Diverted, Cancelled),
            Delayed, List.of(Scheduled, Arrived, Diverted, Cancelled),
            Arrived, List.of(Departed), // Once arrived, it can only depart
            Departed, List.of(),        // Terminal state: No transitions allowed
            Diverted, List.of(Arrived, Cancelled),
            Cancelled, List.of()        // Terminal state: No transitions allowed
    );

    public boolean isValidTransitionTo(FlightStatus nextStatus) {
        // A status can transition to itself (no-op)
        if (this == nextStatus) return true;

        List<FlightStatus> allowed = ALLOWED_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(nextStatus);
    }
}
