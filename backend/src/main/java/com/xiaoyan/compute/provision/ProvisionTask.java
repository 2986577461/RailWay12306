package com.xiaoyan.compute.provision;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("provision_task")
public class ProvisionTask {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long orderId;
    private Integer status;
    private Integer attempt;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private String providerInstanceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
