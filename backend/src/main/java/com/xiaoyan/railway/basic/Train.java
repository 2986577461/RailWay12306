package com.xiaoyan.railway.basic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/** Entity mapped to the {@code train} table. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("train")
public class Train {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String trainNo;
    private String trainType;
    private Long startStationId;
    private Long endStationId;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;
}
