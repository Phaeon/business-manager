package com.businessmanager.dto.stats;

import com.businessmanager.dto.notification.NotificationDtos.NotificationResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StatsDtos {

    public record MonthlyRevenue(String month, BigDecimal amount) {
    }

    public record DashboardSummary(
            BigDecimal revenue,
            BigDecimal pendingAmount,
            long quotesCount,
            long pendingInvoices,
            long paidInvoices,
            long newCustomers,
            long activeCustomers,
            long quotesConverted,
            double conversionRate,
            List<MonthlyRevenue> monthlyRevenue,
            Map<String, Long> projectDistribution,
            List<NotificationResponse> recentActivity
    ) {
    }

    public record SearchResult(String type, UUID id, String title, String subtitle, String link) {
    }

    public record CalendarEvent(String type, UUID id, String title, LocalDate date, String time) {
    }

    public record HistoryEntry(String type, UUID id, String label, LocalDate date, String status, BigDecimal amount) {
    }
}
