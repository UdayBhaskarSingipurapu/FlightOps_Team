package com.project.flightOps.repository;

import com.project.flightOps.entity.Notification;
import com.project.flightOps.entity.User;
import com.project.flightOps.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUserOrderByCreatedDateDesc(User user);

    List<Notification> findByUserAndStatusOrderByCreatedDateDesc(User user, NotificationStatus status);

    long countByUserAndStatus(User user, NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = :status WHERE n.user = :user AND n.status = com.project.flightOps.enums.NotificationStatus.Unread")
    int markAllAsRead(User user, NotificationStatus status);
}