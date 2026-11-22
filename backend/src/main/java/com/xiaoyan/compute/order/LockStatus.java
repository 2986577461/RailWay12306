package com.xiaoyan.compute.order;

import lombok.Getter;

@Getter
public enum LockStatus {
    PROCESSING(0, "锁定中"),
    LOCKED(1, "已锁定"),
    FAILED(2, "锁定失败"),
    RELEASED(3, "已释放");

    private final int code;
    private final String label;

    LockStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static LockStatus fromCode(Integer code) {
        if (code != null) {
            for (LockStatus status : values()) {
                if (status.code == code) return status;
            }
        }
        return PROCESSING;
    }
}
