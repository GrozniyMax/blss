package com.blss.blss.service.camunda.handlers.pickup;

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
@ExternalTaskSubscription("order-pickup: complete")
public class CompletePickup implements ExternalTaskHandler {

    private final OrderService orderService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            orderService.updateStatus(orderId, Status.DONE);
            service.complete(task, Map.of("newStatus", Status.DONE.name(), "processSuccess", true));
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
