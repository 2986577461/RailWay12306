package com.xiaoyan.railway.payment;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.common.BizException;
import com.xiaoyan.railway.inventory.InventoryService;
import com.xiaoyan.railway.order.LockStatus;
import com.xiaoyan.railway.order.Order;
import com.xiaoyan.railway.order.OrderRepository;
import com.xiaoyan.railway.order.OrderStatus;
import com.xiaoyan.railway.payment.dto.MockNotifyCommand;
import com.xiaoyan.railway.payment.dto.PaymentVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PaymentService {
    private final PaymentMapper paymentMapper;
    private final OrderRepository orderRepository;
    private final MockSigner mockSigner;
    private final OrderPaidPublisher orderPaidPublisher;
    private final InventoryService inventoryService;

    public PaymentService(PaymentMapper paymentMapper, OrderRepository orderRepository,
                          MockSigner mockSigner, OrderPaidPublisher orderPaidPublisher,
                          InventoryService inventoryService) {
        this.paymentMapper = paymentMapper;
        this.orderRepository = orderRepository;
        this.mockSigner = mockSigner;
        this.orderPaidPublisher = orderPaidPublisher;
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
        if (order.getLockStatus() == null || order.getLockStatus() != LockStatus.LOCKED.getCode()) {
            throw new BizException("订单尚未锁定座位，暂不可支付");
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
        if (!orderRepository.markPaid(payment.getOrderId())) {
            // 订单已超时取消或锁定失败：抛异常让本事务回滚，支付单保持 PENDING
            throw new BizException("订单已不可支付，回调未生效");
        }
        // 事务提交后再发事件，避免"库已改、消息没发"
        orderPaidPublisher.publishAfterCommit(payment);
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
        refundInternal(order.getId());
        return "退款成功";
    }

    /**
     * Admin bulk refund (by order ID only, no user check).
     * Returns false if order is not in PAID state.
     */
    @Transactional
    public boolean refund(Long orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null || order.getOrderStatus() == OrderStatus.REFUNDED.getCode()) {
            return false;
        }
        if (order.getOrderStatus() != OrderStatus.PAID.getCode()) {
            return false;
        }
        refundInternal(orderId);
        return true;
    }

    private void refundInternal(Long orderId) {
        if (!orderRepository.markRefunded(orderId)) {
            throw new BizException("订单状态已变化，退款失败");
        }
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, orderId));
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.PAID.getCode()) {
            paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                    .set(Payment::getPaymentStatus, PaymentStatus.REFUNDED.getCode())
                    .eq(Payment::getId, payment.getId())
                    .eq(Payment::getPaymentStatus, PaymentStatus.PAID.getCode()));
        }
        Order order = orderRepository.findById(orderId);
        if (order != null) {
            inventoryService.refundOrder(order);
        }
    }
}
