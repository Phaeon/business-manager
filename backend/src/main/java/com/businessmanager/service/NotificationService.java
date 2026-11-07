package com.businessmanager.service;

import com.businessmanager.dto.notification.NotificationDtos.NotificationResponse;
import com.businessmanager.dto.notification.NotificationDtos.NotificationsPayload;
import com.businessmanager.entity.Notification;
import com.businessmanager.entity.NotificationType;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.NotificationMapper;
import com.businessmanager.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public void notify(NotificationType type, String message, String link) {
        String safeMessage = message.length() > 500 ? message.substring(0, 500) : message;
        notificationRepository.save(Notification.builder()
                .type(type)
                .message(safeMessage)
                .link(link)
                .build());
    }

    /** Crée la notification uniquement si aucune n'existe déjà pour ce couple (type, lien). */
    @Transactional
    public boolean notifyOnce(NotificationType type, String message, String link) {
        if (notificationRepository.existsByTypeAndLink(type, link)) {
            return false;
        }
        notify(type, message, link);
        return true;
    }

    @Transactional(readOnly = true)
    public NotificationsPayload list() {
        List<NotificationResponse> items = notificationRepository.findTop30ByOrderByCreatedAtDesc()
                .stream().map(notificationMapper::toResponse).toList();
        return new NotificationsPayload(items, notificationRepository.countByReadFalse());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> recent() {
        return notificationRepository.findTop8ByOrderByCreatedAtDesc()
                .stream().map(notificationMapper::toResponse).toList();
    }

    @Transactional
    public void markRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable : " + id));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead();
    }
}
