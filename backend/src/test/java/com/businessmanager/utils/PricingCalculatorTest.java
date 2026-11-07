package com.businessmanager.utils;

import com.businessmanager.entity.LineItem;
import com.businessmanager.utils.PricingCalculator.Totals;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {

    private static LineItem line(String qty, String price, String vat, String discount) {
        return new LineItem("Prestation", new BigDecimal(qty), new BigDecimal(price),
                new BigDecimal(vat), new BigDecimal(discount));
    }

    @Test
    void lineHt_appliesQuantityAndDiscount() {
        // 3 x 100 = 300, remise 10 % => 270
        assertThat(PricingCalculator.lineHt(line("3", "100", "20", "10"))).isEqualByComparingTo("270.00");
    }

    @Test
    void lineVat_isComputedOnDiscountedAmount() {
        // HT = 270, TVA 20 % => 54
        assertThat(PricingCalculator.lineVat(line("3", "100", "20", "10"))).isEqualByComparingTo("54.00");
    }

    @Test
    void compute_sumsLinesWithMixedVatRates() {
        Totals totals = PricingCalculator.compute(List.of(
                line("1", "1000", "20", "0"),      // HT 1000, TVA 200
                line("2", "50", "5.5", "0")));      // HT 100,  TVA 5.50

        assertThat(totals.ht()).isEqualByComparingTo("1100.00");
        assertThat(totals.vat()).isEqualByComparingTo("205.50");
        assertThat(totals.ttc()).isEqualByComparingTo("1305.50");
    }

    @Test
    void compute_roundsHalfUpToTwoDecimals() {
        // 3 x 0.335 = 1.005 => 1.01 (HALF_UP)
        assertThat(PricingCalculator.lineHt(line("3", "0.335", "0", "0"))).isEqualByComparingTo("1.01");
    }

    @Test
    void compute_returnsZeroForNoLines() {
        Totals totals = PricingCalculator.compute(List.of());
        assertThat(totals.ttc()).isEqualByComparingTo("0");
    }
}
