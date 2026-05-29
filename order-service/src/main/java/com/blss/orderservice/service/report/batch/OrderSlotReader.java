package com.blss.orderservice.service.report.batch;

import com.blss.orderservice.db.order.OrderRepo;
import com.blss.orderservice.domain.order.Order;
import org.springframework.batch.item.ItemReader;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class OrderSlotReader implements ItemReader<Order> {

    private static final int PAGE_SIZE = 500;
    private static final UUID FIRST_PAGE_MARKER = new UUID(0L, 0L);

    private final OrderRepo orderRepo;

    private Instant from;
    private Instant to;
    private final Deque<Order> buffer = new ArrayDeque<>();
    private UUID lastId = FIRST_PAGE_MARKER;
    private boolean exhausted = false;

    public OrderSlotReader(OrderRepo orderRepo) {
        this.orderRepo = orderRepo;
    }

    public void resetForRange(Instant from, Instant to) {
        this.from = from;
        this.to = to;
        this.buffer.clear();
        this.lastId = FIRST_PAGE_MARKER;
        this.exhausted = false;
    }

    @Override
    public Order read() {
        if (buffer.isEmpty() && !exhausted) {
            List<Order> page = orderRepo.findOrdersInRange(from, to, lastId, PAGE_SIZE);
            if (page.isEmpty()) {
                exhausted = true;
                return null;
            }
            buffer.addAll(page);
            lastId = page.get(page.size() - 1).id();
            if (page.size() < PAGE_SIZE) exhausted = true;
        }
        return buffer.poll();
    }
}