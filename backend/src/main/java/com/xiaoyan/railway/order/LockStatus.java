package com.xiaoyan.railway.order;

import lombok.Getter;

/**
 * Async inventory-lock result. Orthogonal to {@link OrderStatus}: an order stays PENDING
 * (payable) while its lock is LOCKED; a PROCESSING order's lock has not resolved yet, and a
 * FAILED order can never be paid.
 */
@Getter
public enum LockStatus {
    PROCESSING(0, "失效"),
    LOCKED(1, "已锁定"),
    FAILED(2, "锁定失败");

    private final int code;
    private final String label;

    LockStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static LockStatus fromCode(int code) {
        for (LockStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown lock status: " + code);
    }
}