package com.blss.orderservice.service.report;

import com.blss.orderservice.config.ReportBatchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportQuartzJob implements Job {

    private final JobLauncher jobLauncher;
    private final org.springframework.batch.core.Job reportJob;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            LocalDate day = LocalDate.now().minusDays(1);
            log.info("Launching Spring Batch reportJob for {}", day);

            JobParameters params = new JobParametersBuilder()
                    .addString(ReportBatchConfig.DAY_PARAM, day.toString())
                    .addLong("runAt", System.currentTimeMillis()) // уникальность запуска
                    .toJobParameters();

            jobLauncher.run(reportJob, params);
            log.info("Spring Batch reportJob submitted for {}", day);
        } catch (Exception e) {
            log.error("Error launching report batch job", e);
            throw new JobExecutionException("Failed to launch report job", e, false);
        }
    }
}