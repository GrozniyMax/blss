package com.blss.orderservice.service.report.batch;

public record SlotContext(int index,
                          OrderSlotReader reader,
                          SlotStatisticWriter writer) {
}