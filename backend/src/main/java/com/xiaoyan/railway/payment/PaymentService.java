package com.xiaoyan.railway.payment;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.common.BizException;
import com.xiaoyan.railway.common.OrderPaidEvent;
import com.xiaoyan.railway.common.RocketTopics;
import com.xiaoyan.railway.inventory.InventoryService;
import com.xiaoyan.railway.order.Order;
import com.xiaoyan.railway.order.OrderRepository;
import com.xiaoyan.railway.order.OrderStatus;
import com.xiaoyan.railway.payment.dto.MockNotifyCommand;
import com.xiaoyan.railway.payment.dto.PaymentVO;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentMapper paymentMapper;
    private final OrderRepository orderRepository;
    private final MockSigner mockSigner;
    private final RocketMQTemplate rocketMQTemplate;
    private final InventoryService inventoryService;

    public PaymentService(PaymentMapper paymentMapper, OrderRepository orderRepository,
                          MockSigner mockSigner, RocketMQTemplate rocketMQTemplate,
                          InventoryService inventoryService) {
        this.paymentMapper = paymentMapper;
        this.orderRepository = orderRepository;
        this.mockSigner = mockSigner;
        this.rocketMQTemplate = rocketMQTemplate;
        this.inventoryService = inventoryService;
    }

    /** Create (or return the existing) pending payment for an order. Idempotent per order. */
    public PaymentVO createPayment(Long userId, String orderNo) {
        Order order = orderRepository.findOrder(userId, orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING.getCode()) {
            throw new BizException("订单不可支付");
        }
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, order.getId()));
        if (payment == null) {
            try {
                payment = Payment.builder()
                        .orderId(order.getId())
                        .paymentNo("P" + IdWorker.getIdStr())
                        .channel("MOCK")
                        .amount(order.getTotalAmount())
                        .paymentStatus(PaymentStatus.PENDING.getCode())
                        .build();
                paymentMapper.insert(payment);
            } catch (DuplicateKeyException e) {
                payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, order.getId()));
            }
        }
        String mockSign = mockSigner.sign(payment.getPaymentNo() + ":" + payment.getAmount());
        return new PaymentVO(payment.getPaymentNo(), payment.getAmount(), mockSign);
    }

    /**
     * Simulated payment-provider async callback. Signature is the authentication here
     * (real WeChat callbacks carry no user token, only a verified sign).
     */
    @Transactional
    public String mockNotify(MockNotifyCommand command) {
        if (!mockSigner.verify(command.paymentNo() + ":" + command.amount(), command.sign())) {
            throw new BizException("回调签名校验失败");
        }
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getPaymentNo, command.paymentNo()));
        if (payment == null) {
            throw new BizException("支付单不存在");
        }
        if (payment.getPaymentStatus() == PaymentStatus.PAID.getCode()) {
            return "SUCCESS"; // 幂等：已处理
        }
        if (payment.getAmount().compareTo(command.amount()) != 0) {
            throw new BizException("回调金额与支付单不一致");
        }
        int updated = paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .set(Payment::getPaymentStatus, PaymentStatus.PAID.getCode())
                .set(Payment::getPaidAt, LocalDateTime.now())
                .set(Payment::getThirdPartyNo, command.thirdPartyNo() != null ? command.thirdPartyNo() : "MOCK_" + IdWorker.getIdStr())
                .eq(Payment::getId, payment.getId())
                .eq(Payment::getPaymentStatus, PaymentStatus.PENDING.getCode()));
        if (updated == 0) {
            return "SUCCESS"; // 并发回调已被另一个线程处理
        }
        orderRepository.markPaid(payment.getOrderId());
        // 事务提交后再发事件，避免"库已改、消息没发"
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rocketMQTemplate.convertAndSend(RocketTopics.ORDER_PAID,
                        new OrderPaidEvent(UUID.randomUUID().toString(), payment.getOrderId(), payment.getPaymentNo()));
            }
        });
        return "SUCCESS";
    }

    /**
     * Mock refund: PAID order → REFUNDED, payment → REFUNDED, and release the locked inventory.
     * Idempotent for already-refunded orders; optimistic update guards concurrent refunds.
     */
    @Transactional
    public String refund(Long userId, String orderNo) {
        Order order = orderRepository.findOrder(userId, orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (order.getOrderStatus() == OrderStatus.REFUNDED.getCode()) {
            return "已退款"; // 幂等
        }
        if (order.getOrderStatus() != OrderStatus.PAID.getCode()) {
            throw new BizException("仅已支付订单可退款");
        }
        if (!orderRepository.markRefunded(order.getId())) {
            throw new BizException("订单状态已变化，退款失败");
        }
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, order.getId()));
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.PAID.getCode()) {
            paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                    .set(Payment::getPaymentStatus, PaymentStatus.REFUNDED.getCode())
                    .eq(Payment::getId, payment.getId())
                    .eq(Payment::getPaymentStatus, PaymentStatus.PAID.getCode()));
        }
        inventoryService.releaseOrder(order);
        return "退款成功";
    }
}
