package com.blss.orderservice.bitrix;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "bitrix", name = {"enabled", "polling-enabled"}, havingValue = "true")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class BitrixDealPollingScheduler {

    BitrixDealStatusSyncService dealStatusSyncService;

    @Scheduled(fixedDelayString = "${bitrix.polling-fixed-delay:30000}")
    public void pollDeals() {
        try {
            dealStatusSyncService.syncOrderStatusesFromDeals();
        } catch (Exception e) {
            log.error("Bitrix polling failed: {}", e.getMessage(), e);
        }
    }
}
