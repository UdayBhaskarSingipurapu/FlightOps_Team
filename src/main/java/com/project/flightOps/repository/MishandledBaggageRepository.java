package com.project.flightOps.repository;

import com.project.flightOps.entity.MishandledBaggage;
import com.project.flightOps.enums.MishandledStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MishandledBaggageRepository extends JpaRepository<MishandledBaggage, String> {

    List<MishandledBaggage> findAllByOrderByReportedDateDesc();

    List<MishandledBaggage> findByStatus(MishandledStatus status);

    Optional<MishandledBaggage> findByBagTagNumber(String bagTagNumber);

    List<MishandledBaggage> findByFlight_FlightIdOrderByReportedDateDesc(String flightId);
}
