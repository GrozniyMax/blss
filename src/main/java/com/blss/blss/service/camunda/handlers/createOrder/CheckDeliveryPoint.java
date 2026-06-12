package com.blss.blss.service.camunda.handlers.createOrder;

import com.blss.blss.db.DeliveryPointRepo;
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
@ExternalTaskSubscription("create-order: check-delivery-point")
public class CheckDeliveryPoint implements ExternalTaskHandler {

    private final DeliveryPointRepo deliveryPointRepo;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var location = CamundaHandlerSupport.uuid(task, "location");
            log.info("Checking delivery point: processInstanceId={}, locationId={}",
                    processInstanceId, location);
            
            if (deliveryPointRepo.findById(location).isEmpty()) {
                log.warn("Delivery point not found: processInstanceId={}, locationId={}",
                        processInstanceId, location);
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", "Delivery point does not exist");
                return;
            }
            log.info("Delivery point verified: processInstanceId={}, locationId={}",
                    processInstanceId, location);
            service.complete(task);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid location UUID: processInstanceId={}, error={}", processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", e.getMessage());
        } catch (Exception e) {
            log.error("Delivery point check failed: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
