package com.xiaoyan.railway.order;

import com.xiaoyan.railway.inventory.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodically cancels pending orders whose payment window has expired, then releases
 * the locked segment inventory back to Redis. Optimistic updates make this safe across
 * overlapping scans / concurrent payments.
 */
@Slf4j
@Component
public class OrderTimeoutJob {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public OrderTimeoutJob(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Scheduled(fixedDelayString = "${railway.order.timeout-scan-seconds:30000}")
    public void cancelExpiredPendingOrders() {
        List<Order> expired = orderRepository.findExpiredPending(LocalDateTime.now(), 200);
        if (expired.isEmpty()) {
            return;
        }
        for (Order order : expired) {
            try {
                if (orderRepository.cancelPending(order.getId())) {
                    inventoryService.releaseOrder(order);
                    log.info("Order auto-cancelled by timeout: orderNo={}", order.getOrderNo());
                }
            } catch (Exception e) {
                log.error("Failed to auto-cancel order {}: {}", order.getOrderNo(), e.getMessage(), e);
            }
        }
    }
}
