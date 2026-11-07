package com.businessmanager.service;

import com.businessmanager.dto.stats.StatsDtos.SearchResult;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.InvoiceRepository;
import com.businessmanager.repository.ProjectRepository;
import com.businessmanager.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Recherche globale (clients, devis, factures, projets) filtrée selon les droits de l'appelant. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static final int LIMIT = 5;

    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProjectRepository projectRepository;

    public List<SearchResult> search(String query, boolean finance, boolean projects) {
        String term = query == null ? "" : query.trim();
        if (term.length() < 2) {
            return List.of();
        }
        Pageable top = PageRequest.of(0, LIMIT);
        List<SearchResult> results = new ArrayList<>();

        customerRepository.search(term, false, top).forEach(c -> results.add(new SearchResult(
                "CUSTOMER", c.getId(), c.getName(),
                c.getCompany() != null && !c.getCompany().isBlank() ? c.getCompany() : c.getEmail(),
                "/clients?q=" + enc(c.getName()))));

        if (finance) {
            quoteRepository.search(term, top).forEach(q -> results.add(new SearchResult(
                    "QUOTE", q.getId(), q.getNumber(),
                    q.getCustomer().getName() + " — " + q.getTotalTtc() + " € TTC",
                    "/devis?q=" + enc(q.getNumber()))));
            invoiceRepository.search(term, top).forEach(i -> results.add(new SearchResult(
                    "INVOICE", i.getId(), i.getNumber(),
                    i.getCustomer().getName() + " — " + i.getTotalTtc() + " € TTC",
                    "/factures?q=" + enc(i.getNumber()))));
        }
        if (projects) {
            projectRepository.search(term, top).forEach(p -> results.add(new SearchResult(
                    "PROJECT", p.getId(), p.getName(), p.getCustomer().getName(),
                    "/projets/" + p.getId())));
        }
        return results;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
