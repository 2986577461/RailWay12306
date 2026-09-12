package com.xiaoyan.railway.order;

import com.xiaoyan.railway.common.ApiResponse;
import com.xiaoyan.railway.common.RocketTopics;
import com.xiaoyan.railway.common.TicketRequestEvent;
import com.xiaoyan.railway.config.UserContext;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final RocketMQTemplate rocketMQTemplate;
    private final OrderRepository orderRepository;
    private final OrderIdGenerator orderIdGenerator;
    private final OrderService orderService;

    public OrderController(RocketMQTemplate rocketMQTemplate, OrderRepository orderRepository,
                           OrderIdGenerator orderIdGenerator, OrderService orderService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.orderRepository = orderRepository;
        this.orderIdGenerator = orderIdGenerator;
        this.orderService = orderService;
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
    public ApiResponse<List<OrderListItemVO>> list() {
        return ApiResponse.ok(orderService.list(UserContext.userId()));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.detail(UserContext.userId(), orderNo));
    }
}
