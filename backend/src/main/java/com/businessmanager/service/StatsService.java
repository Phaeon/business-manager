package com.businessmanager.service;

import com.businessmanager.dto.stats.StatsDtos.DashboardSummary;
import com.businessmanager.dto.stats.StatsDtos.MonthlyRevenue;
import com.businessmanager.entity.InvoiceStatus;
import com.businessmanager.entity.Payment;
import com.businessmanager.entity.ProjectStatus;
import com.businessmanager.entity.QuoteStatus;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.InvoiceRepository;
import com.businessmanager.repository.PaymentRepository;
import com.businessmanager.repository.ProjectRepository;
import com.businessmanager.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private static final List<InvoiceStatus> PENDING = List.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE);

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final QuoteRepository quoteRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    public DashboardSummary dashboard() {
        long quotesCount = quoteRepository.count();
        long converted = quoteRepository.countByStatus(QuoteStatus.INVOICED);
        double conversionRate = quotesCount == 0 ? 0.0 : Math.round(converted * 1000.0 / quotesCount) / 10.0;

        return new DashboardSummary(
                orZero(paymentRepository.sumAmount()),
                orZero(invoiceRepository.sumBalanceByStatusIn(PENDING)),
                quotesCount,
                invoiceRepository.countByStatusIn(PENDING),
                invoiceRepository.countByStatus(InvoiceStatus.PAID),
                customerRepository.countByCreatedAtAfter(Instant.now().minus(30, ChronoUnit.DAYS)),
                customerRepository.countByArchivedFalse(),
                converted,
                conversionRate,
                monthlyRevenue(12),
                projectDistribution(),
                notificationService.recent()
        );
    }

    /** Encaissements des {@code months} derniers mois (mois courant inclus). */
    List<MonthlyRevenue> monthlyRevenue(int months) {
        YearMonth start = YearMonth.now().minusMonths(months - 1L);
        Map<YearMonth, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            totals.put(start.plusMonths(i), BigDecimal.ZERO);
        }
        for (Payment payment : paymentRepository.findByPaidAtGreaterThanEqual(start.atDay(1))) {
            YearMonth month = YearMonth.from(payment.getPaidAt());
            totals.computeIfPresent(month, (k, v) -> v.add(payment.getAmount()));
        }
        List<MonthlyRevenue> result = new ArrayList<>();
        totals.forEach((month, amount) -> result.add(new MonthlyRevenue(month.toString(), amount)));
        return result;
    }

    private Map<String, Long> projectDistribution() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (ProjectStatus status : ProjectStatus.values()) {
            distribution.put(status.name(), projectRepository.countByStatus(status));
        }
        return distribution;
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
