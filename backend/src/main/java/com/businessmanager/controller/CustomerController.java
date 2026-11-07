package com.businessmanager.controller;

import com.businessmanager.dto.customer.CustomerRequest;
import com.businessmanager.dto.customer.CustomerResponse;
import com.businessmanager.dto.stats.StatsDtos.HistoryEntry;
import com.businessmanager.security.SecurityUtils;
import com.businessmanager.service.CustomerService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Lecture : tous les rôles. Écriture / export : administrateur et comptabilité.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Clients")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private static final String WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";

    private final CustomerService customerService;

    @GetMapping
    public Page<CustomerResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean archived,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return customerService.findAll(search, archived, pageable);
    }

    @GetMapping("/export")
    @PreAuthorize(WRITE)
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"clients.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(customerService.exportCsv());
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable UUID id) {
        return customerService.findById(id);
    }

    @GetMapping("/{id}/history")
    public List<HistoryEntry> history(@PathVariable UUID id, Authentication authentication) {
        return customerService.history(id,
                SecurityUtils.canSeeFinance(authentication),
                SecurityUtils.canSeeProjects(authentication));
    }

    @PostMapping
    @PreAuthorize(WRITE)
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE)
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize(WRITE)
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        customerService.setArchived(id, true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/unarchive")
    @PreAuthorize(WRITE)
    public ResponseEntity<Void> unarchive(@PathVariable UUID id) {
        customerService.setArchived(id, false);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
