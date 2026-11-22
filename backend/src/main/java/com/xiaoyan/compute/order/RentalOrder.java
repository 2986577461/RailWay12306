package com.xiaoyan.compute.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("rental_order")
public class RentalOrder {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long skuId;
    private String region;
    private Integer quantity;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer hours;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer status;
    private Integer lockStatus;
    private String lockFailReason;
    private Integer provisionStatus;
    private Integer provisionRetryCount;
    private String provisionError;
    private Long instanceId;
    private String paymentNo;
    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime provisionedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
