package com.blss.orderservice.service.report;


import com.blss.orderservice.domain.order.Order;
import com.blss.orderservice.domain.order.OrderClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

public class StatisticCollector {

    public record Statistic(
            BigDecimal totalPrice,
            long totalCount,
            Map<OrderClass, Long> orderClassCount
    ) {
    }


    private BigDecimal totalPrice = BigDecimal.ZERO;
    private long totalCount = 0;

    private final Map<OrderClass, Long> orderClassCount = new EnumMap<>(OrderClass.class);

    public Statistic collect(Stream<Order> orders) {

        orders
                .peek(order -> totalPrice = totalPrice.add(order.totalAmount()))
                .peek(order -> totalCount++)
                .map(order -> OrderClass.getOrderClass(order.totalAmount()))
                .peek(orderClass -> orderClassCount.merge(orderClass, 1L, Long::sum))
                .forEach(orderClass -> orderClassCount.merge(orderClass, 1L, Long::sum));

        return new Statistic(totalPrice, totalCount, orderClassCount);
    }
}
