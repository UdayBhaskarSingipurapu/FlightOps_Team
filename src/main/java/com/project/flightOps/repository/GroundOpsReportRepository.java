package com.project.flightOps.repository;

import com.project.flightOps.entity.GroundOpsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroundOpsReportRepository extends JpaRepository<GroundOpsReport, String> {

    List<GroundOpsReport> findByScopeOrderByGeneratedDateDesc(String scope);

    List<GroundOpsReport> findAllByOrderByGeneratedDateDesc();

    List<GroundOpsReport> findByGeneratedDateBetweenOrderByGeneratedDateDesc(
            LocalDateTime from, LocalDateTime to);
}
