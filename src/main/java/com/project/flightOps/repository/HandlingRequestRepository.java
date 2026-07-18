package com.project.flightOps.repository;

import com.project.flightOps.entity.Flight;
import com.project.flightOps.entity.HandlingRequest;
import com.project.flightOps.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HandlingRequestRepository extends JpaRepository<HandlingRequest, String> {

    List<HandlingRequest> findByAirlineIdOrderByStatusAsc(String airlineId);

    List<HandlingRequest> findByStatusOrderByStatusAsc(RequestStatus status);

    Optional<HandlingRequest> findByFlight(Flight flight);

    List<HandlingRequest> findByFlight_FlightId(String flightId);

    boolean existsByFlightAndStatusNot(Flight flight, RequestStatus status);

    @Query("SELECT r FROM HandlingRequest r JOIN FETCH r.requestedBy u WHERE u.userId = :userId")
    List<HandlingRequest> findAllByRequestedByUserId(@Param("userId") String userId);
}
