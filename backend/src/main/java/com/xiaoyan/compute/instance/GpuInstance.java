package com.xiaoyan.compute.instance;

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
@TableName("gpu_instance")
public class GpuInstance {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String instanceNo;
    private Long orderId;
    private Long skuId;
    private String region;
    private Integer status;
    private String privateIp;
    private String endpoint;
    private String sshCommand;
    private Integer utilization;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime terminatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
