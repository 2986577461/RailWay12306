package com.xiaoyan.compute.order;

import com.xiaoyan.compute.instance.InstanceService;
import com.xiaoyan.compute.inventory.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTimeoutJob {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final InstanceService instanceService;

    public OrderTimeoutJob(OrderRepository orderRepository, InventoryService inventoryService,
                           InstanceService instanceService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.instanceService = instanceService;
    }

    @Scheduled(fixedDelayString = "${compute.order.timeout-scan-seconds:15000}")
    public void releaseOverdueOrders() {
        LocalDateTime now = LocalDateTime.now();
        for (RentalOrder order : orderRepository.findExpiredPending(now, 100)) {
            try {
                if (orderRepository.markCancelled(order.getId())) {
                    inventoryService.release(order.getId(), order.getSkuId(), order.getStartAt(),
                            order.getHours(), order.getQuantity());
                    log.info("Expired pending order cancelled: {}", order.getOrderNo());
                }
            } catch (Exception e) {
                log.error("Failed to cancel expired order {}", order.getOrderNo(), e);
            }
        }
        for (RentalOrder order : orderRepository.findExpiredRunning(now, 100)) {
            try {
                if (orderRepository.markCompleted(order.getId())) {
                    instanceService.completeByOrder(order.getId());
                    log.info("Running order completed: {}", order.getOrderNo());
                }
            } catch (Exception e) {
                log.error("Failed to complete order {}", order.getOrderNo(), e);
            }
        }
    }
}
