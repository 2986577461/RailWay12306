package com.xiaoyan.compute.order;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING_PAYMENT(1, "待支付"),
    PAID(2, "已支付"),
    PROVISIONING(3, "开通中"),
    RUNNING(4, "运行中"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消"),
    PROVISION_FAILED(7, "开通失败"),
    REFUNDING(8, "退款中"),
    REFUNDED(9, "已退款");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static OrderStatus fromCode(Integer code) {
        if (code != null) {
            for (OrderStatus status : values()) {
                if (status.code == code) return status;
            }
        }
        return PENDING_PAYMENT;
    }
}
