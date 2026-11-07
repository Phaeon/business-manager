package com.businessmanager.entity;

import com.businessmanager.utils.PricingCalculator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Ligne de devis / facture (collection embarquée). */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class LineItem {

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    public BigDecimal getTotalHt() {
        return PricingCalculator.lineHt(this);
    }

    public BigDecimal getTotalVat() {
        return PricingCalculator.lineVat(this);
    }

    public BigDecimal getTotalTtc() {
        return getTotalHt().add(getTotalVat());
    }
}
