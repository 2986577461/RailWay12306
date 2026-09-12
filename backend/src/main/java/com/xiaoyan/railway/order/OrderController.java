package com.xiaoyan.railway.order;

import com.xiaoyan.railway.common.ApiResponse;
import com.xiaoyan.railway.common.RocketTopics;
import com.xiaoyan.railway.common.TicketRequestEvent;
import com.xiaoyan.railway.config.UserContext;
import jakarta.validation.Valid;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final RocketMQTemplate rocketMQTemplate;
    private final OrderRepository orderRepository;
    private final OrderIdGenerator orderIdGenerator;

    public OrderController(RocketMQTemplate rocketMQTemplate, OrderRepository orderRepository,
                           OrderIdGenerator orderIdGenerator) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.orderRepository = orderRepository;
        this.orderIdGenerator = orderIdGenerator;
    }

    @PostMapping("/requests")
    public ApiResponse<Map<String, String>> request(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                    @Valid @RequestBody TicketRequestCommand request) {
        Long userId = UserContext.userId();
        if (request.toSeq() <= request.fromSeq() || request.passengerIds().isEmpty()) {
            return ApiResponse.fail("区间或乘车人参数不合法");
        }
        String requestId = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString() : idempotencyKey;
        OrderRepository.OrderSummary order = orderRepository.findByIdempotencyKey(userId, requestId)
                .orElseGet(() -> orderRepository.create(orderIdGenerator.nextId(), userId, request, requestId));
        TicketRequestEvent event = new TicketRequestEvent(UUID.randomUUID().toString(), requestId, order.id(), userId,
                request.trainRunId(), request.seatTypeId(), request.fromSeq(), request.toSeq(),
                request.passengerIds().size(), request.passengerIds());
        rocketMQTemplate.send(RocketTopics.TICKET_REQUEST, MessageBuilder.withPayload(event).build());
        return ApiResponse.ok(Map.of("requestId", requestId, "orderNo", order.orderNo(), "status", "QUEUED"));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(orderRepository.listByUser(UserContext.userId()).stream().map(this::toView).toList());
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String orderNo) {
        return orderRepository.findByUserAndOrderNo(UserContext.userId(), orderNo)
                .map(order -> ApiResponse.ok(toView(order)))
                .orElseGet(() -> ApiResponse.fail("订单不存在"));
    }

    private Map<String, Object> toView(Order order) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("orderNo", order.getOrderNo());
        view.put("status", statusCode(order.getOrderStatus()));
        view.put("orderStatus", order.getOrderStatus());
        view.put("trainRunId", order.getTrainRunId());
        view.put("fromStationId", order.getFromStationId());
        view.put("toStationId", order.getToStationId());
        view.put("totalAmount", order.getTotalAmount());
        view.put("expireAt", order.getExpireAt());
        view.put("createdAt", order.getCreatedAt());
        return view;
    }

    private String statusCode(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 1 -> "QUEUED";
            case 2 -> "WAIT_PAY";
            case 3 -> "PAID";
            case 4 -> "CANCELLED";
            case 5 -> "FAILED";
            default -> "UNKNOWN";
        };
    }
}
