package com.xiaoyan.railway.payment;

public enum PaymentStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款");

    private final int code;
    private final String label;

    PaymentStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }
}
