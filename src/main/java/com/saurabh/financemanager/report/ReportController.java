package com.saurabh.financemanager.report;

import com.saurabh.financemanager.exception.ErrorResponse;
import com.saurabh.financemanager.report.dto.MonthlySummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@Tag(name = "Reports", description = "Monthly summary of income, expenses and spending against budgets.")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Get the monthly report",
            description = "Defaults to the current month, using the server's time zone. "
                    + "Shows total income, total expenses, balance, and one line per expense category, sorted by spending. "
                    + "Budget status is OK below 80%, WARNING from 80% up to and including 100%, and OVER_LIMIT above 100%. "
                    + "Categories without a budget show NOT_SET. Budgets with no spending still appear with 0 spent.")
    @ApiResponse(responseCode = "200", description = "The report for the month")
    @ApiResponse(responseCode = "400", description = "Month not in the format YYYY-MM",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<MonthlySummary> getMonthlyReport(
            @Parameter(description = "Month in the format YYYY-MM. Optional; without it, the current month is used.", example = "2026-10")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        if (month == null) {
            month = YearMonth.now();
        }
        return ResponseEntity.ok(reportService.getMonthlySummary(month));
    }
}