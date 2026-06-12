package com.blss.blss.service.camunda.handlers.createOrder;

import com.blss.blss.dto.input.OrderCreateRequestDTO;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component("createOrderValidationHandler")
@RequiredArgsConstructor
@ExternalTaskSubscription("create-order: validate")
public class Validation implements ExternalTaskHandler {

    private final Validator validator;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            OrderCreateRequestDTO dto = new OrderCreateRequestDTO(
                    task.getVariable("owner"),
                    CamundaHandlerSupport.uuid(task, "location"),
                    CamundaHandlerSupport.uuidList(task, objectMapper, "productIds")
            );
            log.info("Validating order: processInstanceId={}, owner={}, productCount={}",
                    processInstanceId, dto.owner(), dto.productIds().size());
            
            var violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String errors = CamundaHandlerSupport.validationErrors(violations);
                log.warn("Order validation failed: processInstanceId={}, errors={}", processInstanceId, errors);
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", errors);
                return;
            }
            log.info("Order validated successfully: processInstanceId={}", processInstanceId);
            service.complete(task, Map.of("productCount", dto.productIds().size()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order data: processInstanceId={}, error={}", processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", e.getMessage());
        } catch (Exception e) {
            log.error("Order validation failed: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
