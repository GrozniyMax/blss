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
        try {
            var location = CamundaHandlerSupport.uuid(task, "location");
            if (deliveryPointRepo.findById(location).isEmpty()) {
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", "Delivery point does not exist");
                return;
            }
            service.complete(task);
        } catch (IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
