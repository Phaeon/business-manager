package com.businessmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "documents")
@NoArgsConstructor
@SuperBuilder
public class StoredDocument extends BaseEntity {

    @Column(nullable = false)
    private String fileName;

    /** Nom du fichier sur disque (UUID), jamais exposé au client. */
    @Column(nullable = false)
    private String storedName;

    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String uploadedBy;
}
