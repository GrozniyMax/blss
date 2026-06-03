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
        try {
            OrderCreateRequestDTO dto = new OrderCreateRequestDTO(
                    task.getVariable("owner"),
                    CamundaHandlerSupport.uuid(task, "location"),
                    CamundaHandlerSupport.uuidList(task, objectMapper, "productIds")
            );
            var violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER",
                        CamundaHandlerSupport.validationErrors(violations));
                return;
            }
            service.complete(task, Map.of("productCount", dto.productIds().size()));
        } catch (IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
