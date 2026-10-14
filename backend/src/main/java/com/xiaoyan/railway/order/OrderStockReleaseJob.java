package com.xiaoyan.railway.order;

import com.xiaoyan.railway.inventory.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Retries returning stock for timed-out orders whose release did not complete in
 * {@link OrderTimeoutJob} (Redis unavailable, rebuild in progress). The cancel script is
 * idempotent, so retrying is always safe.
 */
@Slf4j
@Component
public class OrderStockReleaseJob {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public OrderStockReleaseJob(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Scheduled(fixedDelayString = "${railway.order.stock-release-interval-ms:10000}")
    public void releaseUnreleasedStock() {
        List<Order> pending = orderRepository.findCancelledUnreleased(200);
        for (Order order : pending) {
            try {
                inventoryService.cancelUnpaidOrder(order);
                orderRepository.markStockReleased(order.getId());
            } catch (Exception e) {
                log.error("Stock release retry failed for order {}: {}", order.getOrderNo(), e.getMessage(), e);
            }
        }
    }
}
