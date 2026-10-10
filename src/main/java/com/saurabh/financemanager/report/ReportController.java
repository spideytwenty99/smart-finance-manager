package com.saurabh.financemanager.report;

import com.saurabh.financemanager.report.dto.MonthlySummary;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService){
        this.reportService=reportService;
    }

    @GetMapping
    public ResponseEntity<MonthlySummary> getMonthlyReport(@RequestParam(required = false)
                                                               @DateTimeFormat(pattern = "yyyy-MM") YearMonth month){
        if (month==null){
            month= YearMonth.now();
        }
        return ResponseEntity.ok(reportService.getMonthlySummary(month));
    }

}
