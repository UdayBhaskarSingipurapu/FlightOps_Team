package com.project.flightOps.repository;

import com.project.flightOps.entity.AuditLog;
import com.project.flightOps.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByUserOrderByTimestampDesc(User user);

    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime from, LocalDateTime to);

    List<AuditLog> findAllByOrderByTimestampDesc();
}
