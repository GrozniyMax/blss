package com.blss.orderservice.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Планировщик Quartz-задач.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final Scheduler scheduler;
    
    private final JobDetail reportJobDetail;
    
    private final Trigger reportTrigger;

    @PostConstruct
    public void scheduleJobs() throws SchedulerException {
        log.info("Scheduling Quartz jobs");

        scheduler.scheduleJob(reportJobDetail, reportTrigger);
        log.info("Scheduled job: {} with trigger: {}", 
                reportJobDetail.getKey(), reportTrigger.getKey());

        if (!scheduler.isStarted()) {
            scheduler.start();
            log.info("Quartz scheduler started");
        }
    }
}
