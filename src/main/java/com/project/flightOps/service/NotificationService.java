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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // Added Lombok annotation for automatic log field generation
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Called internally by other services to fire a notification
    public void sendNotification(String userId, String message, NotificationCategory category) {
        log.info("Internal request to send notification of category [{}] to user [{}]", category, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Failed to send internal notification. User not found: {}", userId);
                    return new ResourceNotFoundException("User not found: " + userId);
                });

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setCategory(category);
        notification.setStatus(NotificationStatus.Unread);

        Notification savedNotification = notificationRepository.save(notification);
        log.debug("Internal notification created successfully with ID: {}", savedNotification.getNotificationId());
    }

    // External API: POST /api/notifications (for other services or admin triggers)
    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        log.info("API request to create notification for user [{}]", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.error("Failed to create notification via API. User not found: {}", request.getUserId());
                    return new ResourceNotFoundException("User not found: " + request.getUserId());
                });

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(request.getMessage());
        notification.setCategory(request.getCategory());
        notification.setStatus(NotificationStatus.Unread);

        Notification savedNotification = notificationRepository.save(notification);
        log.debug("Notification created successfully via API with ID: {}", savedNotification.getNotificationId());

        return toResponse(savedNotification);
    }

    // GET /api/notifications — all for logged-in user
    public List<NotificationResponse> getAllForUser(String userId) {
        log.info("Fetching all notifications for user [{}]", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Failed to fetch all notifications. User not found: {}", userId);
                    return new ResourceNotFoundException("User not found: " + userId);
                });

        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedDateDesc(user);
        log.debug("Retrieved {} notifications for user [{}]", notifications.size(), userId);

        return notifications.stream().map(this::toResponse).toList();
    }

    // GET /api/notifications/unread
    public List<NotificationResponse> getUnreadForUser(String userId) {
        log.info("Fetching unread notifications for user [{}]", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Failed to fetch unread notifications. User not found: {}", userId);
                    return new ResourceNotFoundException("User not found: " + userId);
                });

        List<Notification> unreadNotifications = notificationRepository
                .findByUserAndStatusOrderByCreatedDateDesc(user, NotificationStatus.Unread);
        log.debug("Retrieved {} unread notifications for user [{}]", unreadNotifications.size(), userId);

        return unreadNotifications.stream().map(this::toResponse).toList();
    }

    // PATCH /api/notifications/{id}/read
    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        log.info("Marking notification [{}] as READ", notificationId);

        Notification notification = findById(notificationId);
        notification.setStatus(NotificationStatus.Read);

        return toResponse(notificationRepository.save(notification));
    }

    // PATCH /api/notifications/read-all
    @Transactional
    public void markAllAsRead(String userId) {
        log.info("Marking all notifications as READ for user [{}]", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Failed to bulk mark as read. User not found: {}", userId);
                    return new ResourceNotFoundException("User not found: " + userId);
                });

        notificationRepository.markAllAsRead(user, NotificationStatus.Read);
        log.debug("Successfully marked all notifications as read for user [{}]", userId);
    }

    // DELETE /api/notifications/{id}
    @Transactional
    public void dismiss(String notificationId) {
        log.info("Dismissing notification [{}]", notificationId);

        Notification notification = findById(notificationId);
        notification.setStatus(NotificationStatus.Dismissed);
        notificationRepository.save(notification);

        log.debug("Notification [{}] successfully marked as DISMISSED", notificationId);
    }

    private Notification findById(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Notification look-up failed. Notification ID [{}] not found", id);
                    return new ResourceNotFoundException("Notification not found: " + id);
                });
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUser().getUserId())
                .userName(n.getUser().getName())
                .message(n.getMessage())
                .category(n.getCategory())
                .status(n.getStatus())
                .createdDate(n.getCreatedDate())
                .build();
    }
}