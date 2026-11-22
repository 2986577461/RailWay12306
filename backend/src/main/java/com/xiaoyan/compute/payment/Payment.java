package com.xiaoyan.compute.payment;

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
@TableName("payment")
public class Payment {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String paymentNo;
    private Long orderId;
    private String channel;
    private BigDecimal amount;
    private Integer status;
    private String thirdPartyNo;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
