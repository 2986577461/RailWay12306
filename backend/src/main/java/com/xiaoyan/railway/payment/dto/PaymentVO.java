package com.xiaoyan.railway.payment.dto;

import java.math.BigDecimal;

public record PaymentVO(String paymentNo, BigDecimal amount, String mockSign) { }
