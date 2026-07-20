package com.project.flightOps.repository;

import com.project.flightOps.entity.BoardingGate;
import com.project.flightOps.enums.GateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardingGateRepository extends JpaRepository<BoardingGate, String> {

    List<BoardingGate> findByFlight_FlightId(String flightId);

    List<BoardingGate> findAllByOrderByOpenTimeAsc();

    List<BoardingGate> findByStatus(GateStatus status);

    boolean existsByGateNumberAndStatusNot(String gateNumber, GateStatus status);

    @Query("SELECT g FROM BoardingGate g " +
            "JOIN FETCH g.assignedAgent u " +
            "LEFT JOIN FETCH g.flight f " +
            "WHERE u.userId = :userId")
    List<BoardingGate> findByAssignedAgentUserId(@Param("userId") String userId);
}
