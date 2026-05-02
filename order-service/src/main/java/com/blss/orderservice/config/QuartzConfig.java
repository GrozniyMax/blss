package com.blss.orderservice.config;

import com.blss.orderservice.service.report.ReportJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import java.util.Properties;

/**
 * Quartz scheduler configuration.
 * Uses RAM job store (no database tables required).
 */
@Slf4j
@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(
            ApplicationContext applicationContext
    ) {
        log.info("Creating Quartz SchedulerFactoryBean");
        
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        
        // Use custom job factory with Spring autowiring
        factory.setJobFactory(new AutowiringSpringBeanJobFactory(applicationContext));

        // Quartz properties
        Properties quartzProps = new Properties();
        quartzProps.setProperty("org.quartz.scheduler.instanceName", "OrderServiceScheduler");
        quartzProps.setProperty("org.quartz.scheduler.instanceId", "AUTO");

        // Thread pool
        quartzProps.setProperty("org.quartz.threadPool.threadCount", "5");
        quartzProps.setProperty("org.quartz.threadPool.threadPriority", "5");
        quartzProps.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");

        // RAM JobStore - no database required
        quartzProps.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        quartzProps.setProperty("org.quartz.jobStore.misfireThreshold", "60000");

        factory.setQuartzProperties(quartzProps);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(false);

        return factory;
    }

    /**
     * JobDetail for report generation - uses Spring bean.
     */
    @Bean
    public JobDetail reportJobDetail() {
        log.info("Creating ReportJob JobDetail");
        return JobBuilder.newJob(ReportJob.class)
                .withIdentity("reportJob", "reporting")
                .withDescription("Generates daily reports")
                .storeDurably()
                .build();
    }

    /**
     * Trigger for report generation - every minute.
     */
    @Bean
    public Trigger reportTrigger() {
        log.info("Creating report trigger with cron: 0 * * * * ? (every minute)");
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 0 * * ?")
                .withMisfireHandlingInstructionFireAndProceed();
        
        return TriggerBuilder.newTrigger()
                .forJob(reportJobDetail())
                .withIdentity("reportTrigger", "reporting")
                .withDescription("Every minute")
                .withSchedule(scheduleBuilder)
                .build();
    }

    /**
     * Custom JobFactory that gets job instances from Spring context.
     */
    public static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {
        
        private final ApplicationContext applicationContext;

        public AutowiringSpringBeanJobFactory(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
            // Get job bean from Spring context (supports constructor injection)
            return applicationContext.getBean(bundle.getJobDetail().getJobClass());
        }
    }
}