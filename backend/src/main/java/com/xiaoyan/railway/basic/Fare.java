package com.xiaoyan.railway.basic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Entity mapped to the {@code fare} table (price of one train, one seat type, one segment). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fare")
public class Fare {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long trainId;
    private Long seatTypeId;
    private Long fromStationId;
    private Long toStationId;
    private BigDecimal price;
}
