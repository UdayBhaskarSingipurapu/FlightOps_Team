package com.project.flightOps.repository;

import com.project.flightOps.entity.BoardingGate;
import com.project.flightOps.enums.GateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardingGateRepository extends JpaRepository<BoardingGate, String> {

    List<BoardingGate> findByFlight_FlightId(String flightId);

    List<BoardingGate> findAllByOrderByOpenTimeAsc();

    List<BoardingGate> findByStatus(GateStatus status);

    boolean existsByGateNumberAndStatusNot(String gateNumber, GateStatus status);
}
