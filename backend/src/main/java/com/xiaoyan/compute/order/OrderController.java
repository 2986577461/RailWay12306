package com.xiaoyan.compute.order;

import com.xiaoyan.compute.common.ApiResponse;
import com.xiaoyan.compute.config.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderService.OrderDetailView> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderService.CreateOrderCommand command) {
        return ApiResponse.ok(orderService.create(UserContext.userId(), command, idempotencyKey));
    }

    @GetMapping
    public ApiResponse<List<OrderService.OrderListItemView>> list() {
        return ApiResponse.ok(orderService.list(UserContext.userId()));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderService.OrderDetailView> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.detail(UserContext.userId(), orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<OrderService.OrderDetailView> cancel(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.cancel(UserContext.userId(), orderNo));
    }

    @PostMapping("/{orderNo}/refund")
    public ApiResponse<OrderService.OrderDetailView> refund(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.refund(UserContext.userId(), orderNo));
    }
}
