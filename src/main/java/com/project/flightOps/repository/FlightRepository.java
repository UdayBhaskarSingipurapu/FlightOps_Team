package com.project.flightOps.repository;

import com.project.flightOps.entity.Flight;
import com.project.flightOps.enums.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, String> {

    List<Flight> findByScheduledArrivalBetweenOrderByScheduledArrivalAsc(
            LocalDateTime start, LocalDateTime end);

    List<Flight> findByAirlineCodeIgnoreCaseOrderByScheduledArrivalAsc(String airlineCode);

    List<Flight> findByStatusOrderByScheduledArrivalAsc(FlightStatus status);

    Optional<Flight> findByFlightNumberAndScheduledArrivalBetween(
            String flightNumber, LocalDateTime start, LocalDateTime end);

    boolean existsByFlightNumberAndScheduledArrivalBetween(
            String flightNumber, LocalDateTime start, LocalDateTime end);
}
