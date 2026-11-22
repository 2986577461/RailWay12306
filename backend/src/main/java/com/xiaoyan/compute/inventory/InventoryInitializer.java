package com.xiaoyan.compute.inventory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryInitializer implements ApplicationRunner {
    private final InventoryService inventoryService;

    public InventoryInitializer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        inventoryService.reloadAll();
        log.info("GPU hourly inventory loaded into Redis");
    }
}
