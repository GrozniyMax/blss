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
        String processInstanceId = task.getProcessInstanceId();
        try {
            var productId = CamundaHandlerSupport.uuid(task, "productId");
            Integer change = CamundaHandlerSupport.integer(task, "change");
            
            log.info("Updating product count: processInstanceId={}, productId={}, change={}",
                    processInstanceId, productId, change);
            
            storeService.getProduct(productId);
            storeService.updateItemsCount(productId, change);
            log.info("Product count updated: processInstanceId={}, productId={}, change={}",
                    processInstanceId, productId, change);
            
            service.complete(task, Map.of("productId", productId.toString(), "processSuccess", true));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid product count update: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_PRODUCT_COUNT", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update product count: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
