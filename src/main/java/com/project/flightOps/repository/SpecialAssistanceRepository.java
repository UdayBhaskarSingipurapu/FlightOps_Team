package com.project.flightOps.repository;

import com.project.flightOps.entity.SpecialAssistance;
import com.project.flightOps.enums.AssistanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialAssistanceRepository extends JpaRepository<SpecialAssistance, String> {

    List<SpecialAssistance> findByFlight_FlightId(String flightId);

    List<SpecialAssistance> findByStatusOrderByStatusAsc(AssistanceStatus status);

    List<SpecialAssistance> findAllByOrderByStatusAsc();

    List<SpecialAssistance> findByAssignedAgent_UserId(String agentId);
}
