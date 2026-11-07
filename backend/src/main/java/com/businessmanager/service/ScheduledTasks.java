package com.businessmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Traitements quotidiens : factures en retard et alertes d'échéance de projets. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final InvoiceService invoiceService;
    private final ProjectService projectService;

    @Scheduled(cron = "0 0 7 * * *")
    public void dailyChecks() {
        int overdue = invoiceService.markOverdueInvoices();
        int deadlines = projectService.notifyUpcomingDeadlines();
        log.info("Contrôles quotidiens : {} facture(s) passée(s) en retard, {} alerte(s) d'échéance", overdue, deadlines);
    }
}
