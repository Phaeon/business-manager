package com.businessmanager.service;

import com.businessmanager.dto.customer.CustomerRequest;
import com.businessmanager.dto.customer.CustomerResponse;
import com.businessmanager.dto.stats.StatsDtos.HistoryEntry;
import com.businessmanager.entity.Customer;
import com.businessmanager.entity.NotificationType;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.CustomerMapper;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.InvoiceRepository;
import com.businessmanager.repository.ProjectRepository;
import com.businessmanager.repository.QuoteRepository;
import com.businessmanager.utils.CsvWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final NotificationService notificationService;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;

    public Page<CustomerResponse> findAll(String search, boolean archived, Pageable pageable) {
        String query = search == null ? "" : search.trim();
        return customerRepository.search(query, archived, pageable).map(customerMapper::toResponse);
    }

    public CustomerResponse findById(UUID id) {
        return customerMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = customerRepository.save(customerMapper.toEntity(request));
        notificationService.notify(NotificationType.NEW_CUSTOMER,
                "Nouveau client : " + customer.getName(), "/clients?q=" + encode(customer.getName()));
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = getOrThrow(id);
        customerMapper.updateEntityFromRequest(request, customer);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void setArchived(UUID id, boolean archived) {
        Customer customer = getOrThrow(id);
        customer.setArchived(archived);
        customerRepository.save(customer);
    }

    @Transactional
    public void delete(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client introuvable : " + id);
        }
        // Une contrainte de clé étrangère (devis/factures/projets) renverra un 409 via le handler global.
        customerRepository.deleteById(id);
    }

    /** Historique des devis, factures et projets d'un client (selon les droits de l'appelant). */
    public List<HistoryEntry> history(UUID id, boolean finance, boolean projects) {
        getOrThrow(id);
        List<HistoryEntry> entries = new ArrayList<>();
        if (finance) {
            quoteRepository.findByCustomerIdOrderByIssueDateDesc(id).forEach(q -> entries.add(
                    new HistoryEntry("QUOTE", q.getId(), "Devis " + q.getNumber(), q.getIssueDate(),
                            q.getStatus().name(), q.getTotalTtc())));
            invoiceRepository.findByCustomerIdOrderByIssueDateDesc(id).forEach(i -> entries.add(
                    new HistoryEntry("INVOICE", i.getId(), "Facture " + i.getNumber(), i.getIssueDate(),
                            i.getStatus().name(), i.getTotalTtc())));
        }
        if (projects) {
            projectRepository.findByCustomerIdOrderByCreatedAtDesc(id).forEach(p -> entries.add(
                    new HistoryEntry("PROJECT", p.getId(), "Projet " + p.getName(),
                            p.getStartDate() != null ? p.getStartDate()
                                    : LocalDate.ofInstant(p.getCreatedAt(), ZoneId.systemDefault()),
                            p.getStatus().name(), p.getBudget())));
        }
        entries.sort(Comparator.comparing(HistoryEntry::date).reversed());
        return entries;
    }

    public byte[] exportCsv() {
        List<List<String>> rows = customerRepository.findAll(Sort.by("name")).stream()
                .map(c -> List.of(
                        nz(c.getName()), nz(c.getCompany()), nz(c.getEmail()), nz(c.getPhone()),
                        nz(c.getAddress()), c.isArchived() ? "Oui" : "Non",
                        c.getCreatedAt() == null ? "" : c.getCreatedAt().toString()))
                .toList();
        return CsvWriter.toBytes(
                List.of("Nom", "Société", "Email", "Téléphone", "Adresse", "Archivé", "Créé le"), rows);
    }

    private Customer getOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
