package com.blss.blss.domain;

import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Информация о товаре в магазине
 */
record Product(

        @Id
        UUID id,
        String name,
        BigDecimal price

        //Здесь могла бы быть ваша реклама
) { }
