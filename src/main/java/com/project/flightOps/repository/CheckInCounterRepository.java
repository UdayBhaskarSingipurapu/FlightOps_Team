package com.project.flightOps.repository;

import com.project.flightOps.entity.CheckInCounter;
import com.project.flightOps.enums.CounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckInCounterRepository extends JpaRepository<CheckInCounter, String> {

    List<CheckInCounter> findByFlight_FlightId(String flightId);

    List<CheckInCounter> findAllByOrderByOpenTimeAsc();

    List<CheckInCounter> findByStatus(CounterStatus status);

    // Prevent assigning same physical counter number to two open flights simultaneously
    boolean existsByCounterNumberAndStatusNot(String counterNumber, CounterStatus status);
}
