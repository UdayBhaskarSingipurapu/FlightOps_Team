package com.project.flightOps.repository;

import com.project.flightOps.entity.BaggageOperation;
import com.project.flightOps.enums.Direction;
import com.project.flightOps.enums.OperationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaggageOperationRepository extends JpaRepository<BaggageOperation, String> {

    List<BaggageOperation> findByFlight_FlightIdOrderByStartTimeDesc(String flightId);

    List<BaggageOperation> findAllByOrderByStartTimeDesc();

    List<BaggageOperation> findByStatus(OperationStatus status);

    boolean existsByFlight_FlightIdAndDirection(String flightId, Direction direction);
}
