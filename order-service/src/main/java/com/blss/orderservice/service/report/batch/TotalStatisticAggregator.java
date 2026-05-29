package com.blss.orderservice.service.report.batch;

import com.blss.orderservice.domain.order.OrderClass;
import com.blss.orderservice.service.report.StatisticCollector.Statistic;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class TotalStatisticAggregator {

    public Statistic aggregate(List<SlotStatisticWriter> slotWriters) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        long totalCount = 0;
        Map<OrderClass, Long> classCount = new EnumMap<>(OrderClass.class);

        for (SlotStatisticWriter w : slotWriters) {
            Statistic s = w.toStatistic();
            totalPrice = totalPrice.add(s.totalPrice());
            totalCount += s.totalCount();
            s.orderClassCount().forEach((k, v) -> classCount.merge(k, v, Long::sum));
        }
        return new Statistic(totalPrice, totalCount, Map.copyOf(classCount));
    }
}