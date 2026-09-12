package com.xiaoyan.railway.payment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entity mapped to the {@code payment} table. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment")
public class Payment {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private String paymentNo;
    private String channel;
    private BigDecimal amount;
    private Integer paymentStatus;
    private String thirdPartyNo;
    private LocalDateTime paidAt;
}
