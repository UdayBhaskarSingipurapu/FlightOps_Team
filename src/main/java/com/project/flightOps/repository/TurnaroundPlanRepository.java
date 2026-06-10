package com.project.flightOps.repository;

import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.TurnaroundPlan;
import com.project.flightOps.enums.TurnaroundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TurnaroundPlanRepository extends JpaRepository<TurnaroundPlan, String> {

    Optional<TurnaroundPlan> findByFlight(Flight flight);

    Optional<TurnaroundPlan> findByFlight_FlightId(String flightId);

    List<TurnaroundPlan> findByStatusOrderByStatusAsc(TurnaroundStatus status);

    boolean existsByFlight(Flight flight);
}
