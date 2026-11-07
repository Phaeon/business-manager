package com.businessmanager.service;

import com.businessmanager.dto.quote.QuoteDtos.QuoteRequest;
import com.businessmanager.dto.quote.QuoteDtos.QuoteResponse;
import com.businessmanager.entity.Customer;
import com.businessmanager.entity.NotificationType;
import com.businessmanager.entity.Quote;
import com.businessmanager.entity.QuoteStatus;
import com.businessmanager.exception.ConflictException;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.QuoteMapper;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.QuoteRepository;
import com.businessmanager.utils.LineItemFactory;
import com.businessmanager.utils.PricingCalculator;
import com.businessmanager.utils.PricingCalculator.Totals;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final CustomerRepository customerRepository;
    private final QuoteMapper quoteMapper;
    private final NumberingService numberingService;
    private final NotificationService notificationService;

    public Page<QuoteResponse> findAll(String search, QuoteStatus status, Pageable pageable) {
        String q = search == null ? "" : search.trim();
        Page<Quote> page = status == null
                ? quoteRepository.search(q, pageable)
                : quoteRepository.searchByStatus(q, status, pageable);
        return page.map(quoteMapper::toResponse);
    }

    public QuoteResponse findById(UUID id) {
        return quoteMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public QuoteResponse create(QuoteRequest request) {
        Customer customer = getCustomer(request.customerId());
        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : LocalDate.now();

        Quote quote = Quote.builder()
                .number(numberingService.nextQuoteNumber(issueDate))
                .customer(customer)
                .status(QuoteStatus.DRAFT)
                .issueDate(issueDate)
                .validUntil(request.validUntil() != null ? request.validUntil() : issueDate.plusDays(30))
                .notes(request.notes())
                .build();
        applyLines(quote, request);
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Transactional
    public QuoteResponse update(UUID id, QuoteRequest request) {
        Quote quote = getOrThrow(id);
        if (quote.getStatus() != QuoteStatus.DRAFT && quote.getStatus() != QuoteStatus.SENT) {
            throw new ConflictException("Seuls les devis brouillon ou envoyés peuvent être modifiés");
        }
        quote.setCustomer(getCustomer(request.customerId()));
        if (request.issueDate() != null) {
            quote.setIssueDate(request.issueDate());
        }
        quote.setValidUntil(request.validUntil());
        quote.setNotes(request.notes());
        applyLines(quote, request);
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    @Transactional
    public QuoteResponse send(UUID id) {
        return transition(id, QuoteStatus.SENT, EnumSet.of(QuoteStatus.DRAFT));
    }

    @Transactional
    public QuoteResponse accept(UUID id) {
        QuoteResponse response = transition(id, QuoteStatus.ACCEPTED, EnumSet.of(QuoteStatus.DRAFT, QuoteStatus.SENT));
        notificationService.notify(NotificationType.QUOTE_ACCEPTED,
                "Devis " + response.number() + " accepté (" + response.customerName() + ")",
                "/devis?q=" + response.number());
        return response;
    }

    @Transactional
    public QuoteResponse reject(UUID id) {
        return transition(id, QuoteStatus.REJECTED, EnumSet.of(QuoteStatus.DRAFT, QuoteStatus.SENT));
    }

    @Transactional
    public QuoteResponse duplicate(UUID id) {
        Quote source = getOrThrow(id);
        LocalDate today = LocalDate.now();

        Quote copy = Quote.builder()
                .number(numberingService.nextQuoteNumber(today))
                .customer(source.getCustomer())
                .status(QuoteStatus.DRAFT)
                .issueDate(today)
                .validUntil(today.plusDays(30))
                .notes(source.getNotes())
                .build();
        copy.getLines().addAll(LineItemFactory.copyOf(source.getLines()));
        applyTotals(copy);
        return quoteMapper.toResponse(quoteRepository.save(copy));
    }

    @Transactional
    public void delete(UUID id) {
        Quote quote = getOrThrow(id);
        if (quote.getStatus() != QuoteStatus.DRAFT && quote.getStatus() != QuoteStatus.REJECTED) {
            throw new ConflictException("Seuls les devis brouillon ou refusés peuvent être supprimés");
        }
        quoteRepository.delete(quote);
    }

    private QuoteResponse transition(UUID id, QuoteStatus target, Set<QuoteStatus> allowedFrom) {
        Quote quote = getOrThrow(id);
        if (!allowedFrom.contains(quote.getStatus())) {
            throw new ConflictException(
                    "Transition impossible : le devis est " + quote.getStatus() + ", attendu " + allowedFrom);
        }
        quote.setStatus(target);
        return quoteMapper.toResponse(quoteRepository.save(quote));
    }

    private void applyLines(Quote quote, QuoteRequest request) {
        quote.getLines().clear();
        quote.getLines().addAll(LineItemFactory.fromRequests(request.lines()));
        applyTotals(quote);
    }

    private void applyTotals(Quote quote) {
        Totals totals = PricingCalculator.compute(quote.getLines());
        quote.setTotalHt(totals.ht());
        quote.setTotalVat(totals.vat());
        quote.setTotalTtc(totals.ttc());
    }

    private Customer getCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
    }

    private Quote getOrThrow(UUID id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devis introuvable : " + id));
    }
}
