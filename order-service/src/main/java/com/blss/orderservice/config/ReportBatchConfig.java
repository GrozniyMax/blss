package com.blss.orderservice.config;

import com.blss.orderservice.db.order.OrderRepo;
import com.blss.orderservice.domain.order.Order;
import com.blss.orderservice.service.report.batch.OrderSlotReader;
import com.blss.orderservice.service.report.batch.SlotContext;
import com.blss.orderservice.service.report.batch.SlotStatisticWriter;
import com.blss.orderservice.service.report.batch.TotalStatisticAggregator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReportBatchConfig {

    public static final String JOB_NAME      = "reportJob";
    public static final String DAY_PARAM     = "day";
    public static final String CTX_STATISTIC = "report.statistic.json";

    private static final int CHUNK_SIZE = 500;
    private static final long SLOT_HOURS = 4;
    private static final int SLOTS      = (int) (24 / SLOT_HOURS);

    private final OrderRepo orderRepo;
    private final ObjectMapper objectMapper;
    private final TotalStatisticAggregator aggregator;


    @Bean
    public List<SlotContext> slotContexts() {
        List<SlotContext> list = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            list.add(new SlotContext(i, new OrderSlotReader(orderRepo), new SlotStatisticWriter()));
        }
        return list;
    }

    @Bean
    public JobExecutionListener slotPreparationListener(List<SlotContext> slotContexts) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                LocalDate day = LocalDate.parse(jobExecution.getJobParameters().getString(DAY_PARAM));

                for (SlotContext slot : slotContexts) {
                    Instant from = day.atStartOfDay()
                            .plusHours(slot.index() * SLOT_HOURS)
                            .toInstant(ZoneOffset.UTC);
                    Instant to = day.atStartOfDay()
                            .plusHours((slot.index() + 1) * SLOT_HOURS)
                            .toInstant(ZoneOffset.UTC);
                    slot.reader().resetForRange(from, to);
                    slot.writer().reset();
                }
            }
        };
    }

    @Bean
    public TaskExecutor reportTaskExecutor() {
        return new SimpleAsyncTaskExecutor("report-batch-");
    }

    @Bean
    public Flow splitFlow(List<SlotContext> slotContexts,
                          JobRepository jobRepository,
                          PlatformTransactionManager txManager,
                          TaskExecutor reportTaskExecutor) {
        Flow[] flows = slotContexts.stream()
                .map(slot -> {
                    Step step = new StepBuilder("slotStep-" + slot.index(), jobRepository)
                            .<Order, Order>chunk(CHUNK_SIZE, txManager)
                            .reader(slot.reader())
                            .writer(slot.writer())
                            .build();
                    return (Flow) new FlowBuilder<SimpleFlow>("flow-" + slot.index())
                            .start(step)
                            .build();
                })
                .toArray(Flow[]::new);

        return new FlowBuilder<SimpleFlow>("splitFlow")
                .split(reportTaskExecutor)
                .add(flows)
                .build();
    }

    @Bean
    public Step aggregateStep(JobRepository jobRepository,
                              PlatformTransactionManager txManager,
                              List<SlotContext> slotContexts) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            List<SlotStatisticWriter> writers = slotContexts.stream()
                    .map(SlotContext::writer)
                    .toList();

            var total = aggregator.aggregate(writers);
            try {
                String json = objectMapper.writeValueAsString(total);
                chunkContext.getStepContext()
                        .getStepExecution()
                        .getJobExecution()
                        .getExecutionContext()
                        .putString(CTX_STATISTIC, json);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Cannot serialize statistic", e);
            }
            log.info("Aggregated daily statistic: {}", total);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("aggregateStep", jobRepository)
                .tasklet(tasklet, txManager)
                .build();
    }

    @Bean("reportJob")
    public Job reportBatchJob(JobRepository jobRepository,
                              Flow splitFlow,
                              Step aggregateStep,
                              JobExecutionListener slotPreparationListener) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(slotPreparationListener)
                .start(splitFlow)
                .next(aggregateStep)
                .end()
                .build();
    }
}