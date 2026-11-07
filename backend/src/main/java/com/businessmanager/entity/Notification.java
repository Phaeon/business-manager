package com.businessmanager.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Notification partagée par toute l'équipe (simplification volontaire :
 * pas de table de lecture par utilisateur).
 */
@Getter
@Setter
@Entity
@Table(name = "notifications")
@NoArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 200)
    private String link;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;
}
