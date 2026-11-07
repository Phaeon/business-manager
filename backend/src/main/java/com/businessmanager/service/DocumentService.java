package com.businessmanager.service;

import com.businessmanager.dto.document.DocumentDtos.DocumentResponse;
import com.businessmanager.entity.Customer;
import com.businessmanager.entity.DocumentCategory;
import com.businessmanager.entity.StoredDocument;
import com.businessmanager.exception.BadRequestException;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.DocumentMapper;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.StoredDocumentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Gestion documentaire : stockage sur disque (volume Docker) + métadonnées en base. */
@Service
@Transactional(readOnly = true)
public class DocumentService {

    public record FileContent(String fileName, String contentType, Resource resource) {
    }

    private final StoredDocumentRepository documentRepository;
    private final CustomerRepository customerRepository;
    private final DocumentMapper documentMapper;
    private final Path root;

    public DocumentService(StoredDocumentRepository documentRepository,
                           CustomerRepository customerRepository,
                           DocumentMapper documentMapper,
                           @Value("${app.storage.path:./uploads}") String storagePath) {
        this.documentRepository = documentRepository;
        this.customerRepository = customerRepository;
        this.documentMapper = documentMapper;
        this.root = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    public Page<DocumentResponse> findAll(UUID customerId, Pageable pageable) {
        Page<StoredDocument> page = customerId == null
                ? documentRepository.findAll(pageable)
                : documentRepository.findByCustomerId(customerId, pageable);
        return page.map(documentMapper::toResponse);
    }

    @Transactional
    public DocumentResponse upload(MultipartFile file, DocumentCategory category, UUID customerId, String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide");
        }
        String original = StringUtils.getFilename(StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "document" : file.getOriginalFilename()));
        if (original == null || original.isBlank() || original.contains("..")) {
            throw new BadRequestException("Nom de fichier invalide");
        }

        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + customerId));
        }

        String storedName = UUID.randomUUID().toString();
        Path target = root.resolve(storedName).normalize();
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer le fichier", e);
        }

        StoredDocument document = StoredDocument.builder()
                .fileName(original)
                .storedName(storedName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .category(category != null ? category : DocumentCategory.OTHER)
                .customer(customer)
                .uploadedBy(uploadedBy)
                .build();
        return documentMapper.toResponse(documentRepository.save(document));
    }

    public FileContent download(UUID id) {
        StoredDocument document = getOrThrow(id);
        Path path = root.resolve(document.getStoredName()).normalize();
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Fichier introuvable sur le stockage");
        }
        return new FileContent(document.getFileName(), document.getContentType(), new FileSystemResource(path));
    }

    @Transactional
    public void delete(UUID id) {
        StoredDocument document = getOrThrow(id);
        documentRepository.delete(document);
        try {
            Files.deleteIfExists(root.resolve(document.getStoredName()).normalize());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de supprimer le fichier", e);
        }
    }

    private StoredDocument getOrThrow(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));
    }
}
