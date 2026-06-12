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

@Slf4j
@Component
@RequiredArgsConstructor
@ExternalTaskSubscription("order-pickup: verify")
public class VerifyPickup implements ExternalTaskHandler {

    private final OrderService orderService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var orderId = CamundaHandlerSupport.uuid(task, "orderId");
            var status = orderService.getStatus(orderId);
            log.info("Verifying order pickup: processInstanceId={}, orderId={}, status={}",
                    processInstanceId, orderId, status);
            
            if (status != Status.READY_FOR_PICKUP) {
                String message = "Order is not ready for pickup. Current status: " + status;
                log.warn("Order pickup verification failed: processInstanceId={}, orderId={}, status={}",
                        processInstanceId, orderId, status);
                service.handleFailure(task, message, message, 0, 0L);
                return;
            }
            log.info("Order pickup verified successfully: processInstanceId={}, orderId={}",
                    processInstanceId, orderId);
            service.complete(task);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order ID for pickup verification: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_PICKUP", e.getMessage());
        } catch (Exception e) {
            log.error("Order pickup verification failed: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
