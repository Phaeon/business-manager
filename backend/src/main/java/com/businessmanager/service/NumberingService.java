package com.businessmanager.service;

import com.businessmanager.repository.InvoiceRepository;
import com.businessmanager.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Génère des numéros séquentiels par année : DEV-2026-0001, FAC-2026-0001.
 * La contrainte UNIQUE en base protège contre un éventuel doublon en cas d'accès concurrent.
 */
@Service
@RequiredArgsConstructor
public class NumberingService {

    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;

    public String nextQuoteNumber(LocalDate date) {
        String prefix = prefix("DEV", date);
        return next(prefix, quoteRepository.findMaxNumber(prefix));
    }

    public String nextInvoiceNumber(LocalDate date) {
        String prefix = prefix("FAC", date);
        return next(prefix, invoiceRepository.findMaxNumber(prefix));
    }

    private static String prefix(String code, LocalDate date) {
        return code + "-" + date.getYear() + "-";
    }

    private static String next(String prefix, String currentMax) {
        int sequence = 1;
        if (currentMax != null) {
            sequence = Integer.parseInt(currentMax.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%04d", sequence);
    }
}
