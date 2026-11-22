package com.xiaoyan.compute.inventory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class InventoryKeys {
    private InventoryKeys() { }

    public static String slot(Long skuId, LocalDateTime slotStart) {
        long epochHour = slotStart.toEpochSecond(ZoneOffset.UTC) / 3600;
        return "compute:inventory:{" + skuId + "}:" + epochHour;
    }

    public static String marker(Long skuId, Long orderId) {
        return "compute:inventory:{" + skuId + "}:lock:" + orderId;
    }

    public static List<String> slots(Long skuId, LocalDateTime startAt, int hours) {
        List<String> keys = new ArrayList<>(hours);
        for (int i = 0; i < hours; i++) {
            keys.add(slot(skuId, startAt.plusHours(i)));
        }
        return keys;
    }
}
