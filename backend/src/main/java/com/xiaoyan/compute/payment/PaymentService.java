package com.xiaoyan.compute.payment;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.compute.common.BizException;
import com.xiaoyan.compute.order.LockStatus;
import com.xiaoyan.compute.order.OrderRepository;
import com.xiaoyan.compute.order.OrderStatus;
import com.xiaoyan.compute.order.RentalOrder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {
    private final PaymentMapper paymentMapper;
    private final OrderRepository orderRepository;
    private final MockSigner mockSigner;
    private final OrderPaidPublisher paidPublisher;

    public PaymentService(PaymentMapper paymentMapper, OrderRepository orderRepository,
                          MockSigner mockSigner, OrderPaidPublisher paidPublisher) {
        this.paymentMapper = paymentMapper;
        this.orderRepository = orderRepository;
        this.mockSigner = mockSigner;
        this.paidPublisher = paidPublisher;
    }

    public PaymentView createPayment(Long userId, String orderNo) {
        RentalOrder order = orderRepository.findOrder(userId, orderNo);
        if (order == null) throw new BizException("订单不存在");
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT.getCode()) {
            throw new BizException("订单不可支付");
        }
        if (order.getLockStatus() == null || order.getLockStatus() != LockStatus.LOCKED.getCode()) {
            throw new BizException("订单正在锁定资源，请稍后再试");
        }
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, order.getId()));
        if (payment == null) {
            try {
                payment = Payment.builder()
                        .id(IdWorker.getId())
                        .paymentNo("PAY-" + IdWorker.getIdStr())
                        .orderId(order.getId())
                        .channel("MOCK")
                        .amount(order.getTotalAmount())
                        .status(PaymentStatus.PENDING.getCode())
                        .createdAt(LocalDateTime.now())
                        .build();
                paymentMapper.insert(payment);
            } catch (DuplicateKeyException ignored) {
                payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, order.getId()));
            }
        }
        String sign = mockSigner.sign(mockSigner.payload(payment.getPaymentNo(), payment.getAmount()));
        return new PaymentView(payment.getPaymentNo(), payment.getAmount(), sign);
    }

    @Transactional
    public String mockNotify(String paymentNo, BigDecimal amount, String thirdPartyNo, String sign) {
        if (!mockSigner.verify(mockSigner.payload(paymentNo, amount), sign)) {
            throw new BizException("支付回调签名校验失败");
        }
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) throw new BizException("支付单不存在");
        if (payment.getStatus() == PaymentStatus.PAID.getCode()) return "SUCCESS";
        if (payment.getAmount().compareTo(amount) != 0) throw new BizException("支付金额不一致");
        int updated = paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .set(Payment::getStatus, PaymentStatus.PAID.getCode())
                .set(Payment::getPaidAt, LocalDateTime.now())
                .set(Payment::getThirdPartyNo, thirdPartyNo == null ? "MOCK-" + IdWorker.getIdStr() : thirdPartyNo)
                .eq(Payment::getId, payment.getId())
                .eq(Payment::getStatus, PaymentStatus.PENDING.getCode()));
        if (updated == 0) return "SUCCESS";
        boolean paid = orderRepository.markPaid(payment.getOrderId(), payment.getPaymentNo());
        if (paid) {
            paidPublisher.publishAfterCommit(payment);
        }
        return "SUCCESS";
    }

    public void refund(RentalOrder order) {
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, order.getId()));
        if (payment != null) {
            paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                    .set(Payment::getStatus, PaymentStatus.REFUNDED.getCode())
                    .set(Payment::getRefundedAt, LocalDateTime.now())
                    .eq(Payment::getId, payment.getId())
                    .eq(Payment::getStatus, PaymentStatus.PAID.getCode()));
        }
    }

    public record PaymentView(String paymentNo, BigDecimal amount, String sign) { }
}
