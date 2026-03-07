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
        // Название товара. Уникально в рамках текущей доменной модели
        String name,
        BigDecimal price
        //Здесь могла бы быть ваша реклама и другая информация о продукте
) { }
