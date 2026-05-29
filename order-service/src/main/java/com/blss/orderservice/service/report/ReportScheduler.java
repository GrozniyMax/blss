package com.blss.orderservice.service.report;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ReportScheduler {

    private ReportService reportService;

//  Сам метод не нужен
//  @Scheduled(cron = "0 0 0 * * *")
    public void generateReport() {
        try {
            log.info("Generating report about yesterday");
            LocalDate date = LocalDate.now().minusDays(1);
            reportService.generateReport(date);
            log.info("Report generated successfully");
        } catch (Exception e) {
            log.error("Error while generating report", e);
        }
    }
}
