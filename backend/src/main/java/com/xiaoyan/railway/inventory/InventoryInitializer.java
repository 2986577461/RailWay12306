package com.xiaoyan.railway.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds Redis segment inventory for active train runs at startup.
 * Uses {@code force=false} so existing counts (already deducted by locks) survive restarts.
 */
@Component
public class InventoryInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InventoryInitializer.class);

    private final InventoryService inventoryService;

    public InventoryInitializer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        inventoryService.reloadAll(false);
        log.info("Redis segment inventory initialized for active train runs");
    }
}
