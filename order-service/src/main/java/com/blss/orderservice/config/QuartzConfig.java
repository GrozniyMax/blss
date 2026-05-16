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

@Slf4j
@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(
            ApplicationContext applicationContext
    ) {
        log.info("Creating Quartz SchedulerFactoryBean");
        
        SchedulerFactoryBean factory = new SchedulerFactoryBean();

        factory.setJobFactory(new AutowiringSpringBeanJobFactory(applicationContext));

        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(false);

        return factory;
    }

    @Bean
    public JobDetail reportJobDetail() {
        log.info("Creating ReportJob JobDetail");
        return JobBuilder.newJob(ReportJob.class)
                .withIdentity("reportJob", "reporting")
                .withDescription("Generates daily reports")
                .storeDurably()
                .build();
    }

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

    public static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

        private final ApplicationContext applicationContext;

        public AutowiringSpringBeanJobFactory(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
            return applicationContext.getBean(bundle.getJobDetail().getJobClass());
        }
    }
}