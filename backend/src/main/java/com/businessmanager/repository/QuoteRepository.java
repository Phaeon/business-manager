package com.businessmanager.repository;

import com.businessmanager.entity.Quote;
import com.businessmanager.entity.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    @Query("""
            select q from Quote q
            where lower(q.number) like lower(concat('%', :q, '%'))
               or lower(q.customer.name) like lower(concat('%', :q, '%'))
            """)
    Page<Quote> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select q from Quote q
            where q.status = :status
              and (lower(q.number) like lower(concat('%', :q, '%'))
               or lower(q.customer.name) like lower(concat('%', :q, '%')))
            """)
    Page<Quote> searchByStatus(@Param("q") String q, @Param("status") QuoteStatus status, Pageable pageable);

    @Query("select max(q.number) from Quote q where q.number like concat(:prefix, '%')")
    String findMaxNumber(@Param("prefix") String prefix);

    long countByStatus(QuoteStatus status);

    List<Quote> findByCustomerIdOrderByIssueDateDesc(UUID customerId);
}
