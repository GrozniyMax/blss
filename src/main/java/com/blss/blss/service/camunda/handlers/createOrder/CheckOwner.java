package com.blss.blss.service.camunda.handlers.createOrder;

import com.blss.blss.service.UserRegistry;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ExternalTaskSubscription("create-order: check-owner")
public class CheckOwner implements ExternalTaskHandler {

    private final UserRegistry userRegistry;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            String owner = task.getVariable("owner");
            log.info("Checking order owner: processInstanceId={}, owner={}", processInstanceId, owner);
            
            if (!userRegistry.existsByUsername(owner)) {
                log.warn("Order owner does not exist: processInstanceId={}, owner={}", processInstanceId, owner);
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_ORDER", "User does not exist");
                return;
            }
            log.info("Order owner verified: processInstanceId={}, owner={}", processInstanceId, owner);
            service.complete(task);
        } catch (Exception e) {
            log.error("Order owner check failed: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
