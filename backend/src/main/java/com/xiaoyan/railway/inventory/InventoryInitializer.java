package com.xiaoyan.railway.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Rebuilds Redis segment inventory from the order ledger for active train runs at startup,
 * so stock held by existing orders survives a Redis restart.
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
        inventoryService.reloadAll();
        log.info("Redis segment inventory rebuilt from order ledger for active train runs");
    }
}
