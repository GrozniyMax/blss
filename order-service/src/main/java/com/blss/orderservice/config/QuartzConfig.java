package com.blss.orderservice.config;

import com.blss.orderservice.service.report.ReportQuartzJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail reportJobDetail() {
        return JobBuilder.newJob(ReportQuartzJob.class)
                .withIdentity("dailyReportJob", "reporting")
                .withDescription("Generates daily report via Spring Batch")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reportTrigger(JobDetail reportJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reportJobDetail)
                .withIdentity("dailyReportTrigger", "reporting")
                .withDescription("Daily at 00:00")
                .withSchedule(
                        CronScheduleBuilder.cronSchedule("0 0 0 * * ?")
                                .withMisfireHandlingInstructionFireAndProceed()
                )
                .build();
    }
}