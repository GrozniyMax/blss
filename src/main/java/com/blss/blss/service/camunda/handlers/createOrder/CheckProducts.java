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
        try {
            var productIds = CamundaHandlerSupport.uuidList(task, objectMapper, "productIds");
            var found = StreamSupport.stream(productRepo.findAllById(productIds).spliterator(), false).toList();
            if (found.size() != productIds.size()) {
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", "Not all products were found");
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
