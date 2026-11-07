package com.businessmanager.repository;

import com.businessmanager.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query("""
            select c from Customer c
            where c.archived = :archived
              and (lower(c.name) like lower(concat('%', :q, '%'))
                or lower(c.email) like lower(concat('%', :q, '%'))
                or lower(coalesce(c.company, '')) like lower(concat('%', :q, '%')))
            """)
    Page<Customer> search(@Param("q") String q, @Param("archived") boolean archived, Pageable pageable);

    long countByArchivedFalse();

    long countByCreatedAtAfter(Instant instant);
}
