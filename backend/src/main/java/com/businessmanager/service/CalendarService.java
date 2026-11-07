package com.businessmanager.service;

import com.businessmanager.dto.stats.StatsDtos.CalendarEvent;
import com.businessmanager.entity.InvoiceStatus;
import com.businessmanager.exception.BadRequestException;
import com.businessmanager.repository.AppointmentRepository;
import com.businessmanager.repository.InvoiceRepository;
import com.businessmanager.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Agrège rendez-vous, échéances de projets et échéances de factures sur une plage de dates. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<InvoiceStatus> OPEN_INVOICES =
            List.of(InvoiceStatus.DRAFT, InvoiceStatus.SENT, InvoiceStatus.OVERDUE);

    private final AppointmentRepository appointmentRepository;
    private final ProjectRepository projectRepository;
    private final InvoiceRepository invoiceRepository;

    public List<CalendarEvent> events(LocalDate from, LocalDate to, boolean finance, boolean projects) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 62) {
            throw new BadRequestException("Plage de dates invalide (62 jours maximum)");
        }
        List<CalendarEvent> events = new ArrayList<>();

        appointmentRepository.findByStartAtBetweenOrderByStartAtAsc(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .forEach(a -> events.add(new CalendarEvent("APPOINTMENT", a.getId(), a.getTitle(),
                        a.getStartAt().toLocalDate(), a.getStartAt().toLocalTime().format(TIME_FORMAT))));

        if (projects) {
            projectRepository.findByStartDateBetween(from, to).forEach(p -> events.add(
                    new CalendarEvent("PROJECT_START", p.getId(), "Début : " + p.getName(), p.getStartDate(), null)));
            projectRepository.findByDueDateBetween(from, to).forEach(p -> events.add(
                    new CalendarEvent("PROJECT_DUE", p.getId(), "Échéance : " + p.getName(), p.getDueDate(), null)));
        }
        if (finance) {
            invoiceRepository.findByDueDateBetweenAndStatusIn(from, to, OPEN_INVOICES).forEach(i -> events.add(
                    new CalendarEvent("INVOICE_DUE", i.getId(), "Échéance facture " + i.getNumber(), i.getDueDate(), null)));
        }

        events.sort(Comparator.comparing(CalendarEvent::date)
                .thenComparing(e -> e.time() == null ? "" : e.time()));
        return events;
    }
}
