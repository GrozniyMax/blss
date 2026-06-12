package com.blss.blss.service.camunda.handlers.createOrder;

import com.blss.blss.db.ProductRepo;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
@ExternalTaskSubscription("create-order: check-products")
public class CheckProducts implements ExternalTaskHandler {

    private final ProductRepo productRepo;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var productIds = CamundaHandlerSupport.uuidList(task, objectMapper, "productIds");
            log.info("Checking products: processInstanceId={}, productCount={}",
                    processInstanceId, productIds.size());
            
            var found = StreamSupport.stream(productRepo.findAllById(productIds).spliterator(), false).toList();
            if (found.size() != productIds.size()) {
                log.warn("Not all products found: processInstanceId={}, requested={}, found={}",
                        processInstanceId, productIds.size(), found.size());
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", "Not all products were found");
                return;
            }
            log.info("Products verified: processInstanceId={}, productCount={}",
                    processInstanceId, found.size());
            service.complete(task);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid product IDs: processInstanceId={}, error={}", processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", e.getMessage());
        } catch (Exception e) {
            log.error("Product check failed: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
