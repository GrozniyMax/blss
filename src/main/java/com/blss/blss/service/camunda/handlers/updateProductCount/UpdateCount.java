package com.blss.blss.service.camunda.handlers.updateProductCount;

import com.blss.blss.service.StoreService;
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
@ExternalTaskSubscription("update-product-count: update")
public class UpdateCount implements ExternalTaskHandler {

    private final StoreService storeService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            var productId = CamundaHandlerSupport.uuid(task, "productId");
            Integer change = CamundaHandlerSupport.integer(task, "change");
            storeService.getProduct(productId);
            storeService.updateItemsCount(productId, change);
            service.complete(task, Map.of("productId", productId.toString(), "processSuccess", true));
        } catch (IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_PRODUCT_COUNT", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
