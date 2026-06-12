package com.blss.blss.service.camunda.handlers.updateProduct;

import com.blss.blss.domain.Product;
import com.blss.blss.dto.input.ProductUpdateRequestDto;
import com.blss.blss.service.StoreService;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ExternalTaskSubscription("update-product: update")
public class UpdateProduct implements ExternalTaskHandler {

    private final StoreService storeService;
    private final Validator validator;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        String processInstanceId = task.getProcessInstanceId();
        try {
            var productId = CamundaHandlerSupport.uuid(task, "productId");
            String name = task.getVariable("name");
            BigDecimal price = new BigDecimal(task.<String>getVariable("price"));
            var dto = new ProductUpdateRequestDto(name, price);
            
            log.info("Updating product: processInstanceId={}, productId={}, name={}, price={}",
                    processInstanceId, productId, name, price);
            
            var violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String errors = CamundaHandlerSupport.validationErrors(violations);
                log.warn("Product update validation failed: processInstanceId={}, errors={}",
                        processInstanceId, errors);
                CamundaHandlerSupport.bpmnError(task, service, "INVALID_PRODUCT_UPDATE", errors);
                return;
            }
            storeService.updateProduct(new Product(productId, name, price));
            log.info("Product updated successfully: processInstanceId={}, productId={}",
                    processInstanceId, productId);
            service.complete(task, Map.of("productId", productId.toString(), "processSuccess", true));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid product update data: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage());
            CamundaHandlerSupport.bpmnError(task, service, "INVALID_PRODUCT_UPDATE", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update product: processInstanceId={}", processInstanceId, e);
            CamundaHandlerSupport.failure(task, service, e);
        }
    }
}
