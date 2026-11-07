package com.businessmanager.service;

import com.businessmanager.dto.invoice.InvoiceDtos.InvoiceResponse;
import com.businessmanager.dto.invoice.InvoiceDtos.PaymentRequest;
import com.businessmanager.entity.Customer;
import com.businessmanager.entity.Invoice;
import com.businessmanager.entity.InvoiceStatus;
import com.businessmanager.entity.LineItem;
import com.businessmanager.entity.Quote;
import com.businessmanager.entity.QuoteStatus;
import com.businessmanager.exception.ConflictException;
import com.businessmanager.mapper.InvoiceMapper;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.InvoiceRepository;
import com.businessmanager.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private NumberingService numberingService;
    @Mock private NotificationService notificationService;
    @Mock private InvoicePdfService pdfService;

    private InvoiceService invoiceService;
    private final UUID invoiceId = UUID.randomUUID();
    private final Customer customer = Customer.builder().id(UUID.randomUUID()).name("ACME").email("a@acme.fr").build();

    @BeforeEach
    void setUp() {
        InvoiceMapper mapper = Mappers.getMapper(InvoiceMapper.class);
        invoiceService = new InvoiceService(invoiceRepository, customerRepository, quoteRepository,
                mapper, numberingService, notificationService, pdfService);
        lenient().when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Invoice invoice(InvoiceStatus status) {
        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .number("FAC-2026-0001")
                .customer(customer)
                .status(status)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalHt(new BigDecimal("100.00"))
                .totalVat(new BigDecimal("20.00"))
                .totalTtc(new BigDecimal("120.00"))
                .build();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        return invoice;
    }

    private static PaymentRequest payment(String amount) {
        return new PaymentRequest(new BigDecimal(amount), LocalDate.now(), "Virement", null);
    }

    @Test
    void pay_fullAmountMarksInvoicePaid() {
        invoice(InvoiceStatus.SENT);

        InvoiceResponse response = invoiceService.pay(invoiceId, payment("120.00"));

        assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.balance()).isEqualByComparingTo("0");
        assertThat(response.payments()).hasSize(1);
    }

    @Test
    void pay_partialAmountKeepsInvoiceOpen() {
        invoice(InvoiceStatus.SENT);

        InvoiceResponse response = invoiceService.pay(invoiceId, payment("50.00"));

        assertThat(response.status()).isEqualTo(InvoiceStatus.SENT);
        assertThat(response.amountPaid()).isEqualByComparingTo("50.00");
        assertThat(response.balance()).isEqualByComparingTo("70.00");
    }

    @Test
    void pay_rejectsAmountAboveBalance() {
        invoice(InvoiceStatus.SENT);

        assertThatThrownBy(() -> invoiceService.pay(invoiceId, payment("120.01")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void pay_rejectsDraftInvoice() {
        invoice(InvoiceStatus.DRAFT);

        assertThatThrownBy(() -> invoiceService.pay(invoiceId, payment("10.00")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_isRefusedOncePartiallyPaid() {
        Invoice invoice = invoice(InvoiceStatus.SENT);
        invoice.setAmountPaid(new BigDecimal("10.00"));

        assertThatThrownBy(() -> invoiceService.cancel(invoiceId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void createFromQuote_requiresAcceptedQuote() {
        UUID quoteId = UUID.randomUUID();
        Quote draft = Quote.builder().id(quoteId).customer(customer).status(QuoteStatus.DRAFT).build();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> invoiceService.createFromQuote(quoteId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void createFromQuote_copiesLinesAndMarksQuoteInvoiced() {
        UUID quoteId = UUID.randomUUID();
        Quote accepted = Quote.builder().id(quoteId).number("DEV-2026-0001").customer(customer)
                .status(QuoteStatus.ACCEPTED).issueDate(LocalDate.now()).build();
        accepted.getLines().add(new LineItem("Site web", BigDecimal.ONE, new BigDecimal("1000"),
                new BigDecimal("20"), BigDecimal.ZERO));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(accepted));
        when(numberingService.nextInvoiceNumber(any(LocalDate.class))).thenReturn("FAC-2026-0001");

        InvoiceResponse response = invoiceService.createFromQuote(quoteId);

        assertThat(response.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(response.quoteNumber()).isEqualTo("DEV-2026-0001");
        assertThat(response.totalTtc()).isEqualByComparingTo("1200.00");
        assertThat(accepted.getStatus()).isEqualTo(QuoteStatus.INVOICED);
    }
}
