package com.blss.blss.service.camunda.handlers.markDelivered;

import com.blss.blss.dto.input.OrderItemDeliveredDto;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

@Slf4j
@Component("markDeliveredValidationHandler")
@RequiredArgsConstructor
@ExternalTaskSubscription("mark-delivered: validate")
public class Validation implements ExternalTaskHandler {

    private final Validator validator;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            var dto = new OrderItemDeliveredDto(
                    CamundaHandlerSupport.uuid(task, "itemId"),
                    task.getVariable("yacheyka")
            );
            var violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_MARK",
                        CamundaHandlerSupport.validationErrors(violations));
                return;
            }
            service.complete(task);
        } catch (IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_MARK", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
