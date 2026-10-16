package com.xiaoyan.railway.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyan.railway.common.ApiResponse;
import com.xiaoyan.railway.common.RocketTopics;
import com.xiaoyan.railway.common.TicketRequestEvent;
import com.xiaoyan.railway.config.UserContext;
import com.xiaoyan.railway.inventory.InventoryRedis;
import com.xiaoyan.railway.order.dto.OrderDetailVO;
import com.xiaoyan.railway.order.dto.OrderListItemVO;
import jakarta.validation.Valid;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final RocketMQTemplate rocketMQTemplate;
    private final OrderIdGenerator orderIdGenerator;
    private final OrderService orderService;
    private final OrderRateLimiter orderRateLimiter;
    private final InventoryRedis inventoryRedis;

    public OrderController(RocketMQTemplate rocketMQTemplate, OrderIdGenerator orderIdGenerator,
                           OrderService orderService, OrderRateLimiter orderRateLimiter,
                           InventoryRedis inventoryRedis) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.orderIdGenerator = orderIdGenerator;
        this.orderService = orderService;
        this.orderRateLimiter = orderRateLimiter;
        this.inventoryRedis = inventoryRedis;
    }

    @PostMapping("/requests")
    public ApiResponse<Map<String, Object>> request(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                    @Valid @RequestBody TicketRequestCommand request) {
        Long userId = UserContext.userId();
        if (request.toSeq() <= request.fromSeq() || request.passengerIds().isEmpty()) {
            return ApiResponse.fail("区间或乘车人参数不合法");
        }
        if (inventoryRedis.isUnavailable()) {
            return ApiResponse.fail("系统繁忙，请稍后再试");
        }
        orderRateLimiter.check(userId);

        String requestId = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString() : idempotencyKey;

        OrderRepository.OrderSummary order = orderService.placeOrder(orderIdGenerator.nextId(), userId, request, requestId);
        if (order.orderStatus() == OrderStatus.CANCELLED.getCode()) {
            return ApiResponse.fail("订单已超时取消，请重新下单");
        }
        TicketRequestEvent event = new TicketRequestEvent(UUID.randomUUID().toString(), requestId, order.id(), userId,
                request.trainRunId(), request.seatTypeId(), request.fromSeq(), request.toSeq(),
                request.passengerIds().size(), request.passengerIds(),
                order.expireAt() == null ? null : order.expireAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

//       //上面的订单即使是从数据库查出来的也必须被消费一次
//       因为第一次新建订单时，消息队列如果宕机，后续从数据库查出原订单却不消费，就无法减库存
        rocketMQTemplate.send(RocketTopics.TICKET_REQUEST, MessageBuilder.withPayload(event).build());
        return ApiResponse.ok(Map.of("requestId", requestId, "orderNo", order.orderNo(),
                "status", "QUEUED", "lockStatus", order.lockStatus()));
    }

    @GetMapping
    public ApiResponse<List<OrderListItemVO>> list() {
        return ApiResponse.ok(orderService.list(UserContext.userId()));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.detail(UserContext.userId(), orderNo));
    }
}