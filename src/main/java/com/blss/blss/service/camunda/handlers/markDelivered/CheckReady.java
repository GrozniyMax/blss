package com.blss.blss.service.camunda.handlers.markDelivered;

import com.blss.blss.db.order.OrderItemRepo;
import com.blss.blss.domain.order.Status;
import com.blss.blss.service.OrderService;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ExternalTaskSubscription("mark-delivered: check-ready")
public class CheckReady implements ExternalTaskHandler {

    private final OrderService orderService;
    private final OrderItemRepo orderItemRepo;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            boolean ready = orderService.getStatus(orderId) == Status.IN_DELIVERY
                    && orderItemRepo.countItemsWithoutYacheyka(orderId) == 0;
            service.complete(task, Map.of("orderReady", ready, "processSuccess", true));
        } catch (IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_MARK", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
