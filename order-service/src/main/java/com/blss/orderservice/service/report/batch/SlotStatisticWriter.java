package com.blss.orderservice.service.report.batch;

import com.blss.orderservice.domain.order.Order;
import com.blss.orderservice.domain.order.OrderClass;
import com.blss.orderservice.service.report.StatisticCollector.Statistic;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Не потокобезопасен — каждый слот работает в ОДНОМ потоке (своём).
 * Параллельность — между разными слотами, а не внутри одного.
 */
public class SlotStatisticWriter implements ItemWriter<Order> {

    private BigDecimal totalPrice = BigDecimal.ZERO;
    private long totalCount = 0;
    private final Map<OrderClass, Long> orderClassCount = new EnumMap<>(OrderClass.class);

    public void reset() {
        totalPrice = BigDecimal.ZERO;
        totalCount = 0;
        orderClassCount.clear();
    }

    @Override
    public void write(Chunk<? extends Order> chunk) {
        for (Order order : chunk) {
            BigDecimal amount = order.totalAmount() == null ? BigDecimal.ZERO : order.totalAmount();
            totalPrice = totalPrice.add(amount);
            totalCount++;
            orderClassCount.merge(OrderClass.getOrderClass(amount), 1L, Long::sum);
        }
    }

    public Statistic toStatistic() {
        return new Statistic(totalPrice, totalCount, Map.copyOf(orderClassCount));
    }
}