package com.blss.blss.service.camunda.handlers.createProduct;

import com.blss.blss.domain.Product;
import com.blss.blss.exception.AlreadyExistsException;
import com.blss.blss.service.StoreService;
import com.blss.blss.service.camunda.handlers.CamundaHandlerSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component("createProductSaverHandler")
@RequiredArgsConstructor
@ExternalTaskSubscription("create-product: store")
public class Saver implements ExternalTaskHandler {

    private final StoreService storeService;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        try {
            String name = task.getVariable("productName");
            String priceStr = task.getVariable("price");
            Object initialCount = task.getVariable("initialCount");

            BigDecimal price = new BigDecimal(priceStr);
            Product product = new Product(null, name, price);
            Integer count = initialCount instanceof Number number ? number.intValue() : null;

            UUID productId = storeService.createProduct(product, count);
            log.info("Product created: id={}", productId);

            service.complete(task, Map.of("productId", productId.toString(), "processSuccess", true));
        } catch (AlreadyExistsException e) {
            CamundaHandlerSupport.bpmnError(task, service, "PRODUCT_ALREADY_EXISTS",
                    "Product already exists");
        } catch (Exception e) {
            log.error("Failed to save product", e);
            service.handleFailure(task, e.getMessage(),
                    ExceptionUtils.getStackTrace(e), 3, 30000L);
        }
    }
}
