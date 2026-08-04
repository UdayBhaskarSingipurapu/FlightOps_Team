package com.project.flightOps.service;

import com.project.flightOps.entity.Notification;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.NotificationCategory;
import com.project.flightOps.enums.NotificationStatus;
import com.project.flightOps.exception.ResourceNotFoundException;
import com.project.flightOps.repository.NotificationRepository;
import com.project.flightOps.repository.UserRepository;
import com.project.flightOps.requestdto.NotificationRequest;
import com.project.flightOps.responsedto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User mockUser;
    private Notification mockNotification;

    private final String userId = "USR-001";
    private final String userEmail = "user@airport.com";
    private final String notificationId = "NOTIF-001";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(userId);
        mockUser.setEmail(userEmail);
        mockUser.setName("Jane Doe");

        mockNotification = new Notification();
        mockNotification.setNotificationId(notificationId);
        mockNotification.setUser(mockUser);
        mockNotification.setMessage("Flight AA101 delayed");
        mockNotification.setCategory(NotificationCategory.FlightSchedule);
        mockNotification.setStatus(NotificationStatus.Unread);
        mockNotification.setCreatedDate(LocalDateTime.now());
    }

    // --- sendNotification Tests ---

    @Test
    void sendNotification_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        notificationService.sendNotification(userId, "Flight AA101 delayed", NotificationCategory.FlightSchedule);

        verify(notificationRepository).save(argThat(n ->
                n.getUser().equals(mockUser)
                        && n.getMessage().equals("Flight AA101 delayed")
                        && n.getCategory() == NotificationCategory.FlightSchedule
                        && n.getStatus() == NotificationStatus.Unread));
    }

    @Test
    void sendNotification_ThrowsResourceNotFoundException_WhenUserMissing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.sendNotification(userId, "Some message", NotificationCategory.Turnaround));

        verify(notificationRepository, never()).save(any());
    }

    // --- create Tests ---

    @Test
    void create_Success() {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setMessage("Baggage delayed on carousel 3");
        request.setCategory(NotificationCategory.Baggage);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        NotificationResponse response = notificationService.create(request);

        assertNotNull(response);
        assertEquals(notificationId, response.getNotificationId());
        assertEquals(userId, response.getUserId());
        assertEquals("Jane Doe", response.getUserName());
        assertEquals(NotificationStatus.Unread, response.getStatus());
        verify(notificationRepository).save(any(Notification.class));
    }


}
