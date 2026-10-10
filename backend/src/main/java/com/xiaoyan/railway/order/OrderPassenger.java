package com.xiaoyan.railway.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Entity mapped to the {@code order_passenger} table — one row per ticket within an order. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_passenger")
public class OrderPassenger {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private Long passengerId;
    private Long seatTypeId;
    private Long seatId;
    private BigDecimal ticketPrice;
    private Integer ticketStatus;
}
