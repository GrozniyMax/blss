package com.blss.orderservice.service.report;

import com.blss.orderservice.db.order.OrderRepo;
import com.blss.orderservice.domain.order.Order;
import com.blss.orderservice.domain.order.OrderClass;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportService {

    private static final int BATCH_SIZE = 500;
    private static final UUID FIRST_PAGE_MARKER = new UUID(0L, 0L);

    OrderRepo orderRepo;

    public StatisticCollector.Statistic generateReport(LocalDate day) {
        ReportAccumulator accumulator = new ReportAccumulator();

        UUID lastOrderId = FIRST_PAGE_MARKER;
        while (true) {
            List<Order> orders = orderRepo.findOrdersForDay(day, lastOrderId, BATCH_SIZE);
            if (orders.isEmpty()) {
                break;
            }

            accumulator.addAll(orders);
            lastOrderId = orders.get(orders.size() - 1).id();

            if (orders.size() < BATCH_SIZE) {
                break;
            }
        }

        StatisticCollector.Statistic statistic = accumulator.toStatistic();
        log.debug("Report generated for {}: {}", day, statistic);
        return statistic;
    }

    private static class ReportAccumulator {

        private BigDecimal totalPrice = BigDecimal.ZERO;
        private long totalCount = 0;
        private final Map<OrderClass, Long> orderClassCount = new EnumMap<>(OrderClass.class);

        void addAll(List<Order> orders) {
            for (Order order : orders) {
                add(order);
            }
        }

        private void add(Order order) {
            BigDecimal amount = order.totalAmount() == null ? BigDecimal.ZERO : order.totalAmount();
            OrderClass orderClass = OrderClass.getOrderClass(amount);

            totalPrice = totalPrice.add(amount);
            totalCount++;
            orderClassCount.merge(orderClass, 1L, Long::sum);
        }

        StatisticCollector.Statistic toStatistic() {
            return new StatisticCollector.Statistic(totalPrice, totalCount, Map.copyOf(orderClassCount));
        }
    }
}
