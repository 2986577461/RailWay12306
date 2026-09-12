package com.xiaoyan.railway.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Body of the simulated payment-provider async callback. */
public record MockNotifyCommand(
        @NotBlank(message = "支付单号不能为空") String paymentNo,
        @NotNull(message = "金额不能为空") @DecimalMin(value = "0.01", message = "金额不合法") BigDecimal amount,
        String thirdPartyNo,
        @NotBlank(message = "签名不能为空") String sign
) { }
