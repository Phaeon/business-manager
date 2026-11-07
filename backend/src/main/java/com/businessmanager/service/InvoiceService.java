package com.businessmanager.service;

import com.businessmanager.dto.invoice.InvoiceDtos.InvoiceRequest;
import com.businessmanager.dto.invoice.InvoiceDtos.InvoiceResponse;
import com.businessmanager.dto.invoice.InvoiceDtos.PaymentRequest;
import com.businessmanager.entity.Customer;
import com.businessmanager.entity.Invoice;
import com.businessmanager.entity.InvoiceStatus;
import com.businessmanager.entity.NotificationType;
import com.businessmanager.entity.Payment;
import com.businessmanager.entity.Quote;
import com.businessmanager.entity.QuoteStatus;
import com.businessmanager.exception.ConflictException;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.InvoiceMapper;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.InvoiceRepository;
import com.businessmanager.repository.QuoteRepository;
import com.businessmanager.utils.CsvWriter;
import com.businessmanager.utils.LineItemFactory;
import com.businessmanager.utils.PricingCalculator;
import com.businessmanager.utils.PricingCalculator.Totals;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    public record PdfFile(String filename, byte[] content) {
    }

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceMapper invoiceMapper;
    private final NumberingService numberingService;
    private final NotificationService notificationService;
    private final InvoicePdfService pdfService;

    @Value("${app.invoice.payment-terms-days:30}")
    private int paymentTermsDays;

    public Page<InvoiceResponse> findAll(String search, InvoiceStatus status, Pageable pageable) {
        String q = search == null ? "" : search.trim();
        Page<Invoice> page = status == null
                ? invoiceRepository.search(q, pageable)
                : invoiceRepository.searchByStatus(q, status, pageable);
        return page.map(invoiceMapper::toResponse);
    }

    public InvoiceResponse findById(UUID id) {
        return invoiceMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.customerId()));
        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : LocalDate.now();

        Invoice invoice = Invoice.builder()
                .number(numberingService.nextInvoiceNumber(issueDate))
                .customer(customer)
                .status(InvoiceStatus.DRAFT)
                .issueDate(issueDate)
                .dueDate(request.dueDate())
                .notes(request.notes())
                .build();
        invoice.getLines().addAll(LineItemFactory.fromRequests(request.lines()));
        applyTotals(invoice);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    /** Conversion automatique d'un devis accepté en facture brouillon. */
    @Transactional
    public InvoiceResponse createFromQuote(UUID quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Devis introuvable : " + quoteId));
        if (quote.getStatus() != QuoteStatus.ACCEPTED) {
            throw new ConflictException("Seul un devis accepté peut être converti en facture");
        }

        LocalDate today = LocalDate.now();
        Invoice invoice = Invoice.builder()
                .number(numberingService.nextInvoiceNumber(today))
                .customer(quote.getCustomer())
                .quote(quote)
                .status(InvoiceStatus.DRAFT)
                .issueDate(today)
                .dueDate(today.plusDays(paymentTermsDays))
                .notes(quote.getNotes())
                .build();
        invoice.getLines().addAll(LineItemFactory.copyOf(quote.getLines()));
        applyTotals(invoice);

        quote.setStatus(QuoteStatus.INVOICED);
        quoteRepository.save(quote);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse update(UUID id, InvoiceRequest request) {
        Invoice invoice = getOrThrow(id);
        requireStatus(invoice, "modifiée", InvoiceStatus.DRAFT);

        invoice.setCustomer(customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.customerId())));
        if (request.issueDate() != null) {
            invoice.setIssueDate(request.issueDate());
        }
        invoice.setDueDate(request.dueDate());
        invoice.setNotes(request.notes());
        invoice.getLines().clear();
        invoice.getLines().addAll(LineItemFactory.fromRequests(request.lines()));
        applyTotals(invoice);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public void delete(UUID id) {
        Invoice invoice = getOrThrow(id);
        requireStatus(invoice, "supprimée", InvoiceStatus.DRAFT);
        invoiceRepository.delete(invoice);
    }

    @Transactional
    public InvoiceResponse send(UUID id) {
        Invoice invoice = getOrThrow(id);
        requireStatus(invoice, "envoyée", InvoiceStatus.DRAFT);
        invoice.setStatus(InvoiceStatus.SENT);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse pay(UUID id, PaymentRequest request) {
        Invoice invoice = getOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.SENT && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new ConflictException("Seule une facture envoyée ou en retard peut être réglée");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(invoice.getBalance()) > 0) {
            throw new ConflictException("Le montant dépasse le solde restant dû (" + invoice.getBalance() + " €)");
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(amount)
                .paidAt(request.paidAt() != null ? request.paidAt() : LocalDate.now())
                .method(request.method())
                .reference(request.reference())
                .build();
        invoice.getPayments().add(payment);
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));

        if (invoice.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        }

        notificationService.notify(NotificationType.PAYMENT_RECEIVED,
                "Paiement de " + amount.toPlainString() + " € reçu pour la facture " + invoice.getNumber(),
                "/factures?q=" + invoice.getNumber());
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse cancel(UUID id) {
        Invoice invoice = getOrThrow(id);
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getAmountPaid().signum() > 0) {
            throw new ConflictException("Une facture réglée (même partiellement) ne peut pas être annulée");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ConflictException("La facture est déjà annulée");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    /** Appelé quotidiennement : passe en retard les factures envoyées dont l'échéance est dépassée. */
    @Transactional
    public int markOverdueInvoices() {
        List<Invoice> late = invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.SENT, LocalDate.now());
        for (Invoice invoice : late) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
            notificationService.notifyOnce(NotificationType.INVOICE_UNPAID,
                    "Facture " + invoice.getNumber() + " impayée (" + invoice.getCustomer().getName() + ")",
                    "/factures?q=" + invoice.getNumber());
        }
        return late.size();
    }

    public PdfFile pdf(UUID id) {
        Invoice invoice = getOrThrow(id);
        return new PdfFile(invoice.getNumber() + ".pdf", pdfService.generate(invoice));
    }

    public byte[] exportCsv() {
        List<List<String>> rows = invoiceRepository.findAll(Sort.by(Sort.Direction.DESC, "issueDate")).stream()
                .map(i -> List.of(
                        i.getNumber(), i.getCustomer().getName(), i.getStatus().name(),
                        i.getIssueDate().toString(), i.getDueDate().toString(),
                        num(i.getTotalHt()), num(i.getTotalVat()), num(i.getTotalTtc()), num(i.getAmountPaid())))
                .toList();
        return CsvWriter.toBytes(
                List.of("Numéro", "Client", "Statut", "Émission", "Échéance", "Total HT", "TVA", "Total TTC", "Payé"),
                rows);
    }

    private void requireStatus(Invoice invoice, String action, InvoiceStatus required) {
        if (invoice.getStatus() != required) {
            throw new ConflictException("Seule une facture au statut " + required + " peut être " + action);
        }
    }

    private void applyTotals(Invoice invoice) {
        Totals totals = PricingCalculator.compute(invoice.getLines());
        invoice.setTotalHt(totals.ht());
        invoice.setTotalVat(totals.vat());
        invoice.setTotalTtc(totals.ttc());
    }

    private Invoice getOrThrow(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + id));
    }

    private static String num(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
