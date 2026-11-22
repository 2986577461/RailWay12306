package com.xiaoyan.compute.instance;

import lombok.Getter;

@Getter
public enum InstanceStatus {
    RUNNING(1, "running"),
    PROVISIONING(2, "provisioning"),
    DEGRADED(3, "degraded"),
    TERMINATED(4, "terminated");

    private final int code;
    private final String label;

    InstanceStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static InstanceStatus fromCode(Integer code) {
        if (code != null) {
            for (InstanceStatus status : values()) {
                if (status.code == code) return status;
            }
        }
        return PROVISIONING;
    }
}
