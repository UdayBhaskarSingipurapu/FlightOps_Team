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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Called internally by other services to fire a notification
    public void sendNotification(String userId, String message, NotificationCategory category) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setCategory(category);
        notification.setStatus(NotificationStatus.Unread);
        notificationRepository.save(notification);
    }

    // External API: POST /api/notifications (for other services or admin triggers)
    @Transactional
    public NotificationResponse create(NotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(request.getMessage());
        notification.setCategory(request.getCategory());
        notification.setStatus(NotificationStatus.Unread);
        return toResponse(notificationRepository.save(notification));
    }

    // GET /api/notifications — all for logged-in user
    public List<NotificationResponse> getAllForUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return notificationRepository.findByUserOrderByCreatedDateDesc(user)
                .stream().map(this::toResponse).toList();
    }

    // GET /api/notifications/unread
    public List<NotificationResponse> getUnreadForUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return notificationRepository
                .findByUserAndStatusOrderByCreatedDateDesc(user, NotificationStatus.Unread)
                .stream().map(this::toResponse).toList();
    }

    // PATCH /api/notifications/{id}/read
    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        Notification notification = findById(notificationId);
        notification.setStatus(NotificationStatus.Read);
        return toResponse(notificationRepository.save(notification));
    }

    // PATCH /api/notifications/read-all
    @Transactional
    public void markAllAsRead(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        notificationRepository.markAllAsRead(user, NotificationStatus.Read);
    }

    // DELETE /api/notifications/{id}
    @Transactional
    public void dismiss(String notificationId) {
        Notification notification = findById(notificationId);
        notification.setStatus(NotificationStatus.Dismissed);
        notificationRepository.save(notification);
    }

    private Notification findById(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
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
