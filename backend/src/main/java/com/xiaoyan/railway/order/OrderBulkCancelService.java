package com.xiaoyan.railway.order;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.admin.dto.BulkCancelCommand;
import com.xiaoyan.railway.inventory.InventoryService;
import com.xiaoyan.railway.payment.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bulk cancel orders for load test cleanup.
 */
@Slf4j
@Service
public class OrderBulkCancelService {
    private final OrderMapper orderMapper;
    private final OrderPassengerMapper orderPassengerMapper;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    public OrderBulkCancelService(OrderMapper orderMapper, OrderPassengerMapper orderPassengerMapper,
                                  OrderRepository orderRepository, InventoryService inventoryService,
                                  PaymentService paymentService) {
        this.orderMapper = orderMapper;
        this.orderPassengerMapper = orderPassengerMapper;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
    }

    /**
     * Cancel all orders created today matching the exact segment and passenger IDs.
     * Handles both PENDING and PAID orders.
     */
    @Transactional
    public int cancelByPassengersAndSegment(BulkCancelCommand command) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        
        // Find matching order_passenger rows
        List<OrderPassenger> matchingPassengers = orderPassengerMapper.selectList(
                Wrappers.<OrderPassenger>lambdaQuery()
                        .in(OrderPassenger::getPassengerId, command.passengerIds())
                        .eq(OrderPassenger::getSeatTypeId, command.seatTypeId())
        );

        if (matchingPassengers.isEmpty()) {
            log.info("No matching passengers found for bulk cancel");
            return 0;
        }

        List<Long> orderIds = matchingPassengers.stream()
                .map(OrderPassenger::getOrderId)
                .distinct()
                .toList();

        // Find orders created today matching the segment
        List<Order> orders = orderMapper.selectList(
                Wrappers.<Order>lambdaQuery()
                        .in(Order::getId, orderIds)
                        .eq(Order::getTrainRunId, command.trainRunId())
                        .eq(Order::getSeatTypeId, command.seatTypeId())
                        .eq(Order::getFromStationId, command.fromStationId())
                        .eq(Order::getToStationId, command.toStationId())
                        .eq(Order::getFromSeq, command.fromSeq())
                        .eq(Order::getToSeq, command.toSeq())
                        .ge(Order::getCreatedAt, startOfDay)
                        .in(Order::getOrderStatus, OrderStatus.PENDING.getCode(), OrderStatus.PAID.getCode())
        );

        if (orders.isEmpty()) {
            log.info("No matching orders found for bulk cancel");
            return 0;
        }

        int cancelled = 0;
        for (Order order : orders) {
            try {
                if (order.getOrderStatus() == OrderStatus.PAID.getCode()) {
                    // Refund PAID orders
                    boolean refunded = paymentService.refund(order.getId());
                    if (refunded) {
                        cancelled++;
                        log.info("Bulk refunded order: orderId={} orderNo={}", order.getId(), order.getOrderNo());
                    }
                } else if (order.getOrderStatus() == OrderStatus.PENDING.getCode()) {
                    // Cancel PENDING orders
                    boolean updated = orderRepository.cancelPending(order.getId());
                    if (updated) {
                        inventoryService.cancelUnpaidOrder(order);
                        orderRepository.markStockReleased(order.getId());
                        cancelled++;
                        log.info("Bulk cancelled order: orderId={} orderNo={}", order.getId(), order.getOrderNo());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to cancel order: orderId={} error={}", order.getId(), e.getMessage(), e);
            }
        }

        log.info("Bulk cancel completed: total={} cancelled={}", orders.size(), cancelled);
        return cancelled;
    }
}
