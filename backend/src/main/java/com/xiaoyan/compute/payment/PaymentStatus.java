package com.xiaoyan.compute.payment;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款"),
    FAILED(3, "支付失败");

    private final int code;
    private final String label;

    PaymentStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
