package com.businessmanager.repository;

import com.businessmanager.entity.Project;
import com.businessmanager.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("""
            select p from Project p
            where lower(p.name) like lower(concat('%', :q, '%'))
               or lower(p.customer.name) like lower(concat('%', :q, '%'))
            """)
    Page<Project> search(@Param("q") String q, Pageable pageable);

    @Query("""
            select p from Project p
            where p.status = :status
              and (lower(p.name) like lower(concat('%', :q, '%'))
               or lower(p.customer.name) like lower(concat('%', :q, '%')))
            """)
    Page<Project> searchByStatus(@Param("q") String q, @Param("status") ProjectStatus status, Pageable pageable);

    long countByStatus(ProjectStatus status);

    List<Project> findByStartDateBetween(LocalDate from, LocalDate to);

    List<Project> findByDueDateBetween(LocalDate from, LocalDate to);

    List<Project> findByDueDateBetweenAndStatusNot(LocalDate from, LocalDate to, ProjectStatus status);

    List<Project> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
