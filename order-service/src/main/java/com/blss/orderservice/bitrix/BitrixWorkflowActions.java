package com.blss.orderservice.bitrix;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class BitrixWorkflowActions {

    BitrixDealStatusSyncService dealStatusSyncService;
    BitrixProperties bitrixProperties;

    public void pollDeals(DelegateExecution execution) {
        if (!bitrixProperties.isEnabled() || !bitrixProperties.isPollingEnabled()) {
            log.debug("Skipping Bitrix polling: integration or polling disabled");
            return;
        }

        try {
            dealStatusSyncService.syncOrderStatusesFromDeals();
        } catch (Exception e) {
            log.error("Bitrix polling failed: {}", e.getMessage(), e);
        }
    }
}
