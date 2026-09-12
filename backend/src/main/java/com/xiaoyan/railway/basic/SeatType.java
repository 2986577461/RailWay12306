package com.xiaoyan.railway.basic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entity mapped to the {@code seat_type} table. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("seat_type")
public class SeatType {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
}
