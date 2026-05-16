package com.blss.orderservice.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Задача генерации ежедневных отчетов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportJob implements Job {

    private final ReportService reportService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Executing scheduled report generation job");
            LocalDate date = LocalDate.now().minusDays(1);
            reportService.generateReport(date);
            log.info("Report generation completed successfully for date: {}", date);
        } catch (Exception e) {
            log.error("Error while executing report generation job", e);
            throw new JobExecutionException("Failed to generate report", e, false);
        }
    }
}
