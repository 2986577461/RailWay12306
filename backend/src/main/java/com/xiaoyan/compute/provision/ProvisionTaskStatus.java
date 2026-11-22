package com.xiaoyan.compute.provision;

import lombok.Getter;

@Getter
public enum ProvisionTaskStatus {
    PENDING(0, "待执行"),
    RUNNING(1, "执行中"),
    RETRYING(2, "等待重试"),
    SUCCEEDED(3, "已成功"),
    FAILED(4, "已失败");

    private final int code;
    private final String label;

    ProvisionTaskStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
