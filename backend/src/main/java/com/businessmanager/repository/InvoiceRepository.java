package com.businessmanager.repository;

import com.businessmanager.entity.Invoice;
import com.businessmanager.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Query("""
            select i from Invoice i
            where lower(i.number) like lower(concat('%', :q, '%'))
               or lower(i.customer.name) like lower(concat('%', :q, '%'))
            """)
    Page<Invoice> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select i from Invoice i
            where i.status = :status
              and (lower(i.number) like lower(concat('%', :q, '%'))
               or lower(i.customer.name) like lower(concat('%', :q, '%')))
            """)
    Page<Invoice> searchByStatus(@Param("q") String q, @Param("status") InvoiceStatus status, Pageable pageable);

    @Query("select max(i.number) from Invoice i where i.number like concat(:prefix, '%')")
    String findMaxNumber(@Param("prefix") String prefix);

    long countByStatus(InvoiceStatus status);

    long countByStatusIn(Collection<InvoiceStatus> statuses);

    /** Montant restant dû sur les factures dans les statuts donnés (null si aucune). */
    @Query("select sum(i.totalTtc - i.amountPaid) from Invoice i where i.status in :statuses")
    BigDecimal sumBalanceByStatusIn(@Param("statuses") Collection<InvoiceStatus> statuses);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    List<Invoice> findByDueDateBetweenAndStatusIn(LocalDate from, LocalDate to, Collection<InvoiceStatus> statuses);

    List<Invoice> findByCustomerIdOrderByIssueDateDesc(UUID customerId);
}
