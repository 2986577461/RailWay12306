package com.xiaoyan.railway.order;

/** Order state machine. This step implements PENDING → PAID; CANCELLED/REFUNDED land in later steps. */
public enum OrderStatus {
    PENDING(1, "待支付"),
    PAID(2, "已支付"),
    CANCELLED(3, "已取消"),
    COMPLETED(4, "已完成"),
    REFUNDED(5, "已退款");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }

    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status: " + code);
    }
}
