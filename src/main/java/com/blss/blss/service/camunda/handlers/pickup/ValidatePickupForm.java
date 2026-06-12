package com.blss.blss.service.camunda.handlers.pickup;

import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ExternalTaskSubscription("order-pickup: validate-form")
public class ValidatePickupForm implements ExternalTaskHandler {

    private static final int MAX_COMMENT_LENGTH = 500;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            List<String> errors = new ArrayList<>();
            Object acceptedValue = task.getVariable("pickupAccepted");
            Boolean accepted = acceptedValue instanceof Boolean value ? value : null;
            String comment = normalize(task.getVariable("pickupComment"));

            log.info("Validating pickup form: processInstanceId={}, accepted={}, commentLength={}",
                    processInstanceId, accepted, comment != null ? comment.length() : 0);

            if (accepted == null) {
                errors.add("pickupAccepted: choose whether the customer accepts the order");
            }
            if (Boolean.FALSE.equals(accepted) && comment.isBlank()) {
                errors.add("pickupComment: provide a rejection reason");
            }
            if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
                errors.add("pickupComment: must not exceed " + MAX_COMMENT_LENGTH + " characters");
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("formValid", errors.isEmpty());
            variables.put("formValidationErrors", String.join("; ", errors));
            variables.put("pickupComment", comment);

            if (!errors.isEmpty()) {
                log.warn("Pickup form validation failed: processInstanceId={}, errors={}",
                        processInstanceId, String.join("; ", errors));
            } else {
                log.info("Pickup form validated successfully: processInstanceId={}", processInstanceId);
            }

            service.complete(task, variables);
        } catch (Exception e) {
            log.error("Pickup form validation failed: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }

    private String normalize(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
