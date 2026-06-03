package com.blss.blss.service.camunda.handlers.markDelivered;

import com.blss.blss.db.order.OrderItemRepo;
import com.blss.blss.exception.NotFoundException;
import com.blss.blss.service.StorageService;
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
@ExternalTaskSubscription("mark-delivered: update-yacheyka")
public class UpdateYacheyka implements ExternalTaskHandler {

    private final StorageService storageService;
    private final OrderItemRepo orderItemRepo;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            var itemId = CamundaHandlerSupport.uuid(task, "itemId");
            String yacheyka = task.getVariable("yacheyka");
            storageService.updateYacheyka(itemId, yacheyka);
            var item = orderItemRepo.findById(itemId)
                    .orElseThrow(() -> new NotFoundException(com.blss.blss.domain.order.OrderItem.class, itemId));
            service.complete(task, Map.of("orderId", item.orderId().toString()));
        } catch (IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_MARK", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
