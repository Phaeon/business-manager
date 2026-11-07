package com.businessmanager.controller;

import com.businessmanager.dto.quote.QuoteDtos.QuoteRequest;
import com.businessmanager.dto.quote.QuoteDtos.QuoteResponse;
import com.businessmanager.entity.QuoteStatus;
import com.businessmanager.service.QuoteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@Tag(name = "Devis")
@SecurityRequirement(name = "bearerAuth")
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping
    public Page<QuoteResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) QuoteStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return quoteService.findAll(search, status, pageable);
    }

    @GetMapping("/{id}")
    public QuoteResponse findById(@PathVariable UUID id) {
        return quoteService.findById(id);
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> create(@Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.create(request));
    }

    @PutMapping("/{id}")
    public QuoteResponse update(@PathVariable UUID id, @Valid @RequestBody QuoteRequest request) {
        return quoteService.update(id, request);
    }

    @PostMapping("/{id}/send")
    public QuoteResponse send(@PathVariable UUID id) {
        return quoteService.send(id);
    }

    @PostMapping("/{id}/accept")
    public QuoteResponse accept(@PathVariable UUID id) {
        return quoteService.accept(id);
    }

    @PostMapping("/{id}/reject")
    public QuoteResponse reject(@PathVariable UUID id) {
        return quoteService.reject(id);
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<QuoteResponse> duplicate(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.duplicate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        quoteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
