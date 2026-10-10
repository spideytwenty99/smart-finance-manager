package com.saurabh.financemanager.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record MonthlySummary (
       YearMonth month,
       BigDecimal totalIncome,
       BigDecimal totalExpenses,
       BigDecimal balance,

       List<SpentPerCategory> spentPerCategoriesList

){
}
