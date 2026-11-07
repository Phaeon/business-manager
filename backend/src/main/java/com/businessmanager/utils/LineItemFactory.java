package com.businessmanager.utils;

import com.businessmanager.dto.common.LineItemDtos.LineItemRequest;
import com.businessmanager.entity.LineItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class LineItemFactory {

    private LineItemFactory() {
    }

    public static List<LineItem> fromRequests(List<LineItemRequest> requests) {
        List<LineItem> lines = new ArrayList<>();
        for (LineItemRequest r : requests) {
            lines.add(new LineItem(
                    r.description().trim(),
                    r.quantity(),
                    r.unitPrice(),
                    r.vatRate(),
                    r.discountPercent() == null ? BigDecimal.ZERO : r.discountPercent()));
        }
        return lines;
    }

    public static List<LineItem> copyOf(List<LineItem> source) {
        List<LineItem> lines = new ArrayList<>();
        for (LineItem l : source) {
            lines.add(new LineItem(l.getDescription(), l.getQuantity(), l.getUnitPrice(),
                    l.getVatRate(), l.getDiscountPercent()));
        }
        return lines;
    }
}
