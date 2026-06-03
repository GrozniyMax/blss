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
        try {
            String name = task.getVariable("name");
            String address = task.getVariable("address");
            var dto = new DeliveryPointCreateRequestDto(name, address);
            var violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_POINT",
                        CamundaHandlerSupport.validationErrors(violations));
                return;
            }
            var created = registry.createDeliveryPoint(new DeliveryPoint(null, name, address));
            service.complete(task, Map.of(
                    "deliveryPointId", created.id().toString(),
                    "processSuccess", true
            ));
        } catch (AlreadyExistsException | IllegalArgumentException e) {
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_DELIVERY_POINT", e.getMessage());
        } catch (Exception e) {
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
