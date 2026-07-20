package com.project.flightOps.repository;

import com.project.flightOps.entity.CheckInCounter;
import com.project.flightOps.enums.CounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckInCounterRepository extends JpaRepository<CheckInCounter, String> {

    List<CheckInCounter> findByFlight_FlightId(String flightId);

    List<CheckInCounter> findAllByOrderByOpenTimeAsc();

    List<CheckInCounter> findByStatus(CounterStatus status);

    // Prevent assigning same physical counter number to two open flights simultaneously
    boolean existsByCounterNumberAndStatusNot(String counterNumber, CounterStatus status);

    @Query("SELECT c FROM CheckInCounter c " +
            "JOIN FETCH c.assignedAgent u " +
            "LEFT JOIN FETCH c.flight f " +
            "WHERE u.userId = :userId")
    List<CheckInCounter> findByAssignedAgentUserId(@Param("userId") String userId);
}
