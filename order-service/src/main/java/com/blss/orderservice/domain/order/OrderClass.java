package com.blss.orderservice.domain.order;

import java.math.BigDecimal;

public enum OrderClass {

    SMALL(1000, 10000),

    MEDIUM(10001, 50000),

    LARGE(50001, 100000),

    PREMIUM(100001, 200000),

    ELITE(200001, 300000),

    BUSINESS(300001, 400000),

    CORPORATE(400001, 500000),

    VIP(500001, 600000),

    PLATINUM(600001, 800000),

    DIAMOND(800001, 1000000);

    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;

    OrderClass(long minPrice, long maxPrice) {
        this.minPrice = BigDecimal.valueOf(minPrice);
        this.maxPrice = BigDecimal.valueOf(maxPrice);
    }

    public static OrderClass getOrderClass(BigDecimal price) {
        for (OrderClass orderClass : OrderClass.values()) {
            if (price.compareTo(orderClass.minPrice) >= 0 && price.compareTo(orderClass.maxPrice) <= 0) {
                return orderClass;
            }
        }
        if (price.compareTo(OrderClass.DIAMOND.maxPrice) > 0) {
            return OrderClass.DIAMOND;
        } else {
            return OrderClass.SMALL;
        }
    }


}
