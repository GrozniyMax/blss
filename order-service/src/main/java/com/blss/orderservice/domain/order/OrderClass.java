package com.blss.orderservice.domain.order;

import java.math.BigDecimal;

/**
 * Класс заказа (по стоимости)
 */
public enum OrderClass {

    /**
     * Малый класс заказа (1000 - 10000)
     */
    SMALL(1000, 10000),

    /**
     * Средний класс заказа (10001 - 50000)
     */
    MEDIUM(10001, 50000),

    /**
     * Большой класс заказа (50001 - 100000)
     */
    LARGE(50001, 100000),

    /**
     * Премиум класс заказа (100001 - 200000)
     */
    PREMIUM(100001, 200000),

    /**
     * Элитный класс заказа (200001 - 300000)
     */
    ELITE(200001, 300000),

    /**
     * Бизнес класс заказа (300001 - 400000)
     */
    BUSINESS(300001, 400000),

    /**
     * Корпоративный класс заказа (400001 - 500000)
     */
    CORPORATE(400001, 500000),

    /**
     * VIP класс заказа (500001 - 600000)
     */
    VIP(500001, 600000),

    /**
     * Платиновый класс заказа (600001 - 800000)
     */
    PLATINUM(600001, 800000),

    /**
     * Бриллиантовый класс заказа (800001 - 1000000)
     */
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
