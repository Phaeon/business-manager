package com.businessmanager.repository;

import com.businessmanager.entity.Notification;
import com.businessmanager.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findTop30ByOrderByCreatedAtDesc();

    List<Notification> findTop8ByOrderByCreatedAtDesc();

    long countByReadFalse();

    boolean existsByTypeAndLink(NotificationType type, String link);

    @Modifying
    @Query("update Notification n set n.read = true where n.read = false")
    int markAllRead();
}
