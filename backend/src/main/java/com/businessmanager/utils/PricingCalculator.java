package com.businessmanager.utils;

import com.businessmanager.entity.LineItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Calcul des montants HT / TVA / TTC (arrondi commercial à 2 décimales ligne par ligne). */
public final class PricingCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private PricingCalculator() {
    }

    public record Totals(BigDecimal ht, BigDecimal vat, BigDecimal ttc) {
    }

    /** Total HT d'une ligne : quantité x prix unitaire x (1 - remise %). */
    public static BigDecimal lineHt(LineItem line) {
        BigDecimal gross = line.getQuantity().multiply(line.getUnitPrice());
        BigDecimal factor = HUNDRED.subtract(line.getDiscountPercent());
        return gross.multiply(factor).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /** TVA d'une ligne : HT x taux %. */
    public static BigDecimal lineVat(LineItem line) {
        return lineHt(line).multiply(line.getVatRate()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public static Totals compute(List<LineItem> lines) {
        BigDecimal ht = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (LineItem line : lines) {
            ht = ht.add(lineHt(line));
            vat = vat.add(lineVat(line));
        }
        return new Totals(ht, vat, ht.add(vat));
    }
}
