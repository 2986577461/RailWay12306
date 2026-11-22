package com.xiaoyan.compute.payment;

import com.xiaoyan.compute.common.ApiResponse;
import com.xiaoyan.compute.config.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderNo}/payment")
    public ApiResponse<PaymentService.PaymentView> create(@PathVariable String orderNo) {
        return ApiResponse.ok(paymentService.createPayment(UserContext.userId(), orderNo));
    }

    @PostMapping("/payments/mock-notify")
    public ApiResponse<String> notify(@Valid @RequestBody MockNotifyCommand command) {
        return ApiResponse.ok(paymentService.mockNotify(command.paymentNo(), command.amount(),
                command.thirdPartyNo(), command.sign()));
    }

    public record MockNotifyCommand(
            @NotBlank String paymentNo,
            @NotNull BigDecimal amount,
            String thirdPartyNo,
            @NotBlank String sign
    ) { }
}
