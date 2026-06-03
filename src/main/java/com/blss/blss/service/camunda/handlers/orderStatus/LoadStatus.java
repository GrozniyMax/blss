package com.blss.blss.service.camunda.handlers.orderStatus;

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
@ExternalTaskSubscription("order-status: load")
public class LoadStatus implements ExternalTaskHandler {

    private final OrderService orderService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            var status = orderService.getStatus(orderId);
            service.complete(task, Map.of("currentStatus", status.name()));
        } catch (IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_STATUS_ACTION", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
