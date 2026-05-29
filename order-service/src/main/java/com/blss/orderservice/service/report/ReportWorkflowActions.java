package com.blss.orderservice.service.report;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportWorkflowActions {

    ReportService reportService;

    public void generateYesterdayReport(DelegateExecution execution) {
        var date = LocalDate.now().minusDays(1);
        log.info("Generating report from BPMN process for date: {}", date);
        reportService.generateReport(date);
    }
}
