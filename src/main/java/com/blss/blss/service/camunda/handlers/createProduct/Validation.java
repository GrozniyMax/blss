package com.blss.blss.service.camunda.handlers.createProduct;

import com.blss.blss.db.ProductRepo;
import com.blss.blss.dto.input.ProductCreateRequestDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ExternalTaskSubscription("create-product: validate")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Validation implements ExternalTaskHandler {

    Validator validator;
    ProductRepo productRepo;

    @Override
    public void execute(ExternalTask task, ExternalTaskService service) {
        ProductCreateRequestDto dto = buildDto(task);

        Set<ConstraintViolation<ProductCreateRequestDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
            log.warn("Validation failed: {}", errors);
            service.handleBpmnError(task, "VALIDATION_FAILED", errors,
                Map.of("validationErrors", errors));
            return;
        }

        boolean exists = productRepo.findByName(dto.name()).isPresent();
        log.info("Product '{}' exists: {}", dto.name(), exists);

        service.complete(task, Map.of("productExists", exists));
    }

    private ProductCreateRequestDto buildDto(ExternalTask task) {
        String name = task.getVariable("productName");
        String priceStr = task.getVariable("price");
        Long initialCount = task.getVariable("initialCount");

        BigDecimal price = null;
        try {
            price = priceStr != null ? new BigDecimal(priceStr) : null;
        } catch (NumberFormatException ignored) {
            // оставим null — Bean Validation поймает через @NotNull
        }

        Integer count = initialCount != null ? initialCount.intValue() : null;

        return new ProductCreateRequestDto(name, price, count);
    }
}
