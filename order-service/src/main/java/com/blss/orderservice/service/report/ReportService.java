package com.blss.orderservice.service.report;

import com.blss.orderservice.db.order.OrderItemRepo;
import com.blss.orderservice.db.order.OrderRepo;
import com.blss.orderservice.domain.order.Order;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportService {

    private static final int BATCH_SIZE = 500;
    private static final UUID ZERO_UUID = new UUID(0L, 0L);

    OrderRepo orderRepo;

    public StatisticCollector.Statistic generateReport(LocalDate day) {
        StatisticCollector.Statistic statistic;

        try (Stream<Order> orders = streamOrdersForDay(day)) {
            StatisticCollector collector = new StatisticCollector();
            statistic = collector.collect(orders);
            log.debug("Report generated: {}", statistic);
        }

        return statistic;
    }

    public Stream<Order> streamOrdersForDay(LocalDate day) {
        return streamOrdersForDay(day, BATCH_SIZE);
    }

    public Stream<Order> streamOrdersForDay(LocalDate day, int batchSize) {
        Iterator<Order> iterator = new Iterator<>() {
            private UUID lastId = ZERO_UUID;
            private Iterator<Order> current = List.<Order>of().iterator();
            private boolean exhausted = false;

            @Override
            public boolean hasNext() {
                if (current.hasNext()) {
                    return true;
                }
                if (exhausted) {
                    return false;
                }
                List<Order> batch = orderRepo.findOrdersForDay(day, lastId, batchSize);
                if (batch.isEmpty()) {
                    exhausted = true;
                    return false;
                }
                lastId = batch.get(batch.size() - 1).id();
                if (batch.size() < batchSize) {
                    exhausted = true;
                }
                current = batch.iterator();
                return current.hasNext();
            }

            @Override
            public Order next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                return current.next();
            }
        };

        Spliterator<Order> spliterator = Spliterators.spliteratorUnknownSize(
                iterator,
                Spliterator.ORDERED | Spliterator.NONNULL
        );
        return StreamSupport.stream(spliterator, false);
    }
}