package com.blss.orderservice.service.report;

import com.blss.orderservice.config.ReportBatchConfig;
import com.blss.orderservice.service.report.StatisticCollector.Statistic;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final JobLauncher jobLauncher;

    @Qualifier("reportJob")
    private final Job reportBatchJob;

    private final ObjectMapper objectMapper;

    public Statistic generateReport(LocalDate day) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString(ReportBatchConfig.DAY_PARAM, day.toString())
                    .addLong("runAt", System.currentTimeMillis())
                    .toJobParameters();

            log.info("Launching reportBatchJob for {}", day);
            JobExecution execution = jobLauncher.run(reportBatchJob, params);

            if (execution.getStatus() != BatchStatus.COMPLETED) {
                throw new IllegalStateException(
                        "Report job finished with status " + execution.getStatus());
            }

            String json = execution.getExecutionContext()
                    .getString(ReportBatchConfig.CTX_STATISTIC);
            return objectMapper.readValue(json, Statistic.class);

        } catch (Exception e) {
            log.error("Failed to run report job for {}", day, e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }
}