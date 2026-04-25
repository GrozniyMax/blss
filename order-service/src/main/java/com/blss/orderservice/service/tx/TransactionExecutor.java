package com.blss.orderservice.service.tx;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionExecutor {

    TransactionOperations transactionOperations;

    public <T> T inTransaction(Supplier<T> action) {
        return transactionOperations.execute(status -> action.get());
    }

    public void inTransaction(Runnable action) {
        transactionOperations.executeWithoutResult(status -> action.run());
    }
}
