package com.xiaoyan.compute.order;

import lombok.Getter;

@Getter
public enum ProvisionStatus {
    NOT_STARTED(0, "未开始"),
    QUEUED(1, "排队中"),
    RUNNING(2, "开通中"),
    SUCCEEDED(3, "已开通"),
    FAILED(4, "开通失败");

    private final int code;
    private final String label;

    ProvisionStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ProvisionStatus fromCode(Integer code) {
        if (code != null) {
            for (ProvisionStatus status : values()) {
                if (status.code == code) return status;
            }
        }
        return NOT_STARTED;
    }
}
