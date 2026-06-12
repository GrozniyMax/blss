package com.blss.blss.service.camunda.handlers.deliveryPoint;

import com.blss.blss.domain.DeliveryPoint;
import com.blss.blss.dto.input.DeliveryPointCreateRequestDto;
import com.blss.blss.exception.AlreadyExistsException;
import com.blss.blss.service.DeliveryPointRegistry;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
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
@Component
@RequiredArgsConstructor
@ExternalTaskSubscription("delivery-point: create")
public class CreateDeliveryPoint implements ExternalTaskHandler {

    private final DeliveryPointRegistry registry;
    private final Validator validator;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            String name = task.getVariable("name");
            String address = task.getVariable("address");
            var dto = new DeliveryPointCreateRequestDto(name, address);
            log.info("Creating delivery point: processInstanceId={}, name={}, address={}",
                    processInstanceId, name, address);
            
            var violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String errors = CamundaHandlerSupport.validationErrors(violations);
                log.warn("Delivery point validation failed: processInstanceId={}, errors={}",
                        processInstanceId, errors);
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_POINT", errors);
                return;
            }
            var created = registry.createDeliveryPoint(new DeliveryPoint(null, name, address));
            log.info("Delivery point created: processInstanceId={}, id={}",
                    processInstanceId, created.id());
            service.complete(task, Map.of(
                    "deliveryPointId", created.id().toString(),
                    "processSuccess", true
            ));
        } catch (AlreadyExistsException e) {
            log.warn("Delivery point already exists: processInstanceId={}", processInstanceId);
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_POINT", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create delivery point: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
