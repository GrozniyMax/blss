package com.blss.blss.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Информация о товаре в магазине
 */
@Table("product")
public record Product(
        @Id
        UUID id,
        String name,
        BigDecimal price
        //Здесь могла бы быть ваша реклама и другая информация о продукте
) { }
