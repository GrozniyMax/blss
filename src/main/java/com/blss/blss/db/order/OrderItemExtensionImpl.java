package com.blss.blss.db.order;

import com.blss.blss.domain.order.OrderItem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderItemExtensionImpl implements OrderItemExtension {

    NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<UUID> create(List<OrderItem> items) {
        createAll(items);

        return getIds(items.get(0).orderId());
    }

    private void createAll(List<OrderItem> items) {
        var sql = "INSERT INTO order_item (order_id, product_id) VALUES (:orderId, :productId)";

        var params = items.stream()
                .map(item -> new MapSqlParameterSource()
                                .addValue("orderId", item.orderId())
                                .addValue("productId", item.productId()))
                        .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, params);
    }

    private List<UUID> getIds(UUID orderId) {
        var sql = "SELECT id FROM order_item WHERE order_id = :orderId AND yacheyka IS NULL";

        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("orderId", orderId), UUID.class);
    }
}
