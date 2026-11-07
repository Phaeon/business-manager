package com.businessmanager.repository;

import com.businessmanager.entity.StoredDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StoredDocumentRepository extends JpaRepository<StoredDocument, UUID> {

    Page<StoredDocument> findByCustomerId(UUID customerId, Pageable pageable);
}
