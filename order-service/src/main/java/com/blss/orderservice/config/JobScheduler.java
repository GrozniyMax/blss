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

    @Lazy
    private final Scheduler scheduler;
    
    @Lazy
    private final JobDetail reportJobDetail;
    
    @Lazy
    private final Trigger reportTrigger;

    @PostConstruct
    public void scheduleJobs() throws SchedulerException {
        log.info("Scheduling Quartz jobs");

        // Schedule the job
        scheduler.scheduleJob(reportJobDetail, reportTrigger);
        log.info("Scheduled job: {} with trigger: {}", 
                reportJobDetail.getKey(), reportTrigger.getKey());

        // Start scheduler if not already started
        if (!scheduler.isStarted()) {
            scheduler.start();
            log.info("Quartz scheduler started");
        }
    }
}
