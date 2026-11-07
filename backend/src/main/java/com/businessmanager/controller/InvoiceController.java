package com.businessmanager.controller;

import com.businessmanager.dto.invoice.InvoiceDtos.InvoiceRequest;
import com.businessmanager.dto.invoice.InvoiceDtos.InvoiceResponse;
import com.businessmanager.dto.invoice.InvoiceDtos.PaymentRequest;
import com.businessmanager.entity.InvoiceStatus;
import com.businessmanager.service.InvoiceService;
import com.businessmanager.service.InvoiceService.PdfFile;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Factures")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public Page<InvoiceResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return invoiceService.findAll(search, status, pageable);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"factures.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(invoiceService.exportCsv());
    }

    @GetMapping("/{id}")
    public InvoiceResponse findById(@PathVariable UUID id) {
        return invoiceService.findById(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        PdfFile file = invoiceService.pdf(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file.content());
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @PostMapping("/from-quote/{quoteId}")
    public ResponseEntity<InvoiceResponse> createFromQuote(@PathVariable UUID quoteId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createFromQuote(quoteId));
    }

    @PutMapping("/{id}")
    public InvoiceResponse update(@PathVariable UUID id, @Valid @RequestBody InvoiceRequest request) {
        return invoiceService.update(id, request);
    }

    @PostMapping("/{id}/send")
    public InvoiceResponse send(@PathVariable UUID id) {
        return invoiceService.send(id);
    }

    @PostMapping("/{id}/payments")
    public InvoiceResponse pay(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
        return invoiceService.pay(id, request);
    }

    @PostMapping("/{id}/cancel")
    public InvoiceResponse cancel(@PathVariable UUID id) {
        return invoiceService.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
