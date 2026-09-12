package com.xiaoyan.railway.payment;

import com.xiaoyan.railway.common.ApiResponse;
import com.xiaoyan.railway.config.UserContext;
import com.xiaoyan.railway.payment.dto.MockNotifyCommand;
import com.xiaoyan.railway.payment.dto.PaymentVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Create payment for an order (login required). */
    @PostMapping("/{orderNo}")
    public ApiResponse<PaymentVO> create(@PathVariable String orderNo) {
        return ApiResponse.ok(paymentService.createPayment(UserContext.userId(), orderNo));
    }

    /** Simulated provider callback — public, secured by signature, not by login. */
    @PostMapping("/{orderNo}/mock-notify")
    public ApiResponse<String> mockNotify(@PathVariable String orderNo, @Valid @RequestBody MockNotifyCommand command) {
        return ApiResponse.ok(paymentService.mockNotify(command));
    }

    /** Refund a paid order (login required) — releases locked inventory. */
    @PostMapping("/{orderNo}/refund")
    public ApiResponse<String> refund(@PathVariable String orderNo) {
        return ApiResponse.ok(paymentService.refund(UserContext.userId(), orderNo));
    }
}
