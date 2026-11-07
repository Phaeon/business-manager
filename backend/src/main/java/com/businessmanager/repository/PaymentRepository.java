package com.businessmanager.repository;

import com.businessmanager.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** Somme de tous les encaissements (null si aucun paiement). */
    @Query("select sum(p.amount) from Payment p")
    BigDecimal sumAmount();

    List<Payment> findByPaidAtGreaterThanEqual(LocalDate from);
}
