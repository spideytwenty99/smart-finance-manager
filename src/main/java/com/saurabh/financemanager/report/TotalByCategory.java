package com.saurabh.financemanager.report;

import java.math.BigDecimal;
import java.util.UUID;

public record TotalByCategory(
        UUID categoryId,
        String categoryName,
        BigDecimal totalSpent


) {
}
