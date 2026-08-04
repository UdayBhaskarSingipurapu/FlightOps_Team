package com.project.flightOps.service;

import com.project.flightOps.entity.AuditLog;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.Role;
import com.project.flightOps.enums.UserStatus;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.AuditLogRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.AuditLogRequest;
import com.project.flightOps.responsedto.AuditLogResponse;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private User mockUser;
    private AuditLog mockAuditLog;

    private final String userId = "USR-1";
    private final String userEmail = "user@airport.com";
    private final String auditId = "AUD-1";
    private final String action = "LOGIN";
    private final String entityType = "AUTH";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(userId);
        mockUser.setName("Jane Doe");
        mockUser.setEmail(userEmail);
        mockUser.setRole(Role.Admin);
        mockUser.setStatus(UserStatus.Active);

        mockAuditLog = new AuditLog();
        mockAuditLog.setAuditId(auditId);
        mockAuditLog.setUser(mockUser);
        mockAuditLog.setAction(action);
        mockAuditLog.setEntityType(entityType);
        mockAuditLog.setTimestamp(LocalDateTime.now());
    }

    // --- log Method Tests ---

    @Test
    void log_Success_SavesAuditLog() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(mockAuditLog);

        auditLogService.log(userId, action, entityType);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void log_SilentFail_WhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        auditLogService.log(userId, action, entityType);

        verify(auditLogRepository, never()).save(any());
    }

    // --- create Method Tests ---

    @Test
    void create_Success() {
        AuditLogRequest request = new AuditLogRequest();
        request.setUserId(userId);
        request.setAction(action);
        request.setEntityType(entityType);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(mockAuditLog);

        AuditLogResponse response = auditLogService.create(request);

        assertNotNull(response);
        assertEquals(auditId, response.getAuditId());
        assertEquals(userEmail, response.getUserEmail());
        assertEquals(mockUser.getName(), response.getUserName());
        assertEquals(Role.Admin.name(), response.getUserRole());
        assertEquals(action, response.getAction());
        assertEquals(entityType, response.getEntityType());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

}
