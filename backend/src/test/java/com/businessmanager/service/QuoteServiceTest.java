package com.businessmanager.service;

import com.businessmanager.dto.quote.QuoteDtos.QuoteResponse;
import com.businessmanager.entity.Customer;
import com.businessmanager.entity.LineItem;
import com.businessmanager.entity.NotificationType;
import com.businessmanager.entity.Quote;
import com.businessmanager.entity.QuoteStatus;
import com.businessmanager.exception.ConflictException;
import com.businessmanager.mapper.QuoteMapper;
import com.businessmanager.repository.CustomerRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock private QuoteRepository quoteRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private NumberingService numberingService;
    @Mock private NotificationService notificationService;

    private QuoteService quoteService;
    private final UUID quoteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        QuoteMapper mapper = Mappers.getMapper(QuoteMapper.class);
        quoteService = new QuoteService(quoteRepository, customerRepository, mapper, numberingService, notificationService);
    }

    private Quote quote(QuoteStatus status) {
        Quote quote = Quote.builder()
                .id(quoteId)
                .number("DEV-2026-0001")
                .customer(Customer.builder().id(UUID.randomUUID()).name("ACME").email("a@acme.fr").build())
                .status(status)
                .issueDate(LocalDate.now())
                .build();
        quote.getLines().add(new LineItem("Site web", BigDecimal.ONE, new BigDecimal("1000"),
                new BigDecimal("20"), BigDecimal.ZERO));
        quote.setTotalHt(new BigDecimal("1000.00"));
        quote.setTotalVat(new BigDecimal("200.00"));
        quote.setTotalTtc(new BigDecimal("1200.00"));
        return quote;
    }

    @Test
    void accept_movesDraftToAcceptedAndNotifies() {
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote(QuoteStatus.DRAFT)));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));

        QuoteResponse response = quoteService.accept(quoteId);

        assertThat(response.status()).isEqualTo(QuoteStatus.ACCEPTED);
        verify(notificationService).notify(eq(NotificationType.QUOTE_ACCEPTED), anyString(), anyString());
    }

    @Test
    void reject_isForbiddenOnceAccepted() {
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote(QuoteStatus.ACCEPTED)));

        assertThatThrownBy(() -> quoteService.reject(quoteId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_isForbiddenOnceSent() {
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote(QuoteStatus.SENT)));

        assertThatThrownBy(() -> quoteService.delete(quoteId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void duplicate_createsDraftCopyWithNewNumberAndSameTotals() {
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote(QuoteStatus.ACCEPTED)));
        when(numberingService.nextQuoteNumber(any(LocalDate.class))).thenReturn("DEV-2026-0002");
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));

        QuoteResponse copy = quoteService.duplicate(quoteId);

        assertThat(copy.number()).isEqualTo("DEV-2026-0002");
        assertThat(copy.status()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(copy.totalTtc()).isEqualByComparingTo("1200.00");
        assertThat(copy.lines()).hasSize(1);
    }
}
