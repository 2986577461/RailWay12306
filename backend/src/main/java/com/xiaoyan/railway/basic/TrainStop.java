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

/** Entity mapped to the {@code train_stop} table. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("train_stop")
public class TrainStop {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long trainId;
    private Long stationId;
    private Integer stopSeq;
    private LocalTime arriveTime;
    private LocalTime departTime;
    private Integer stopMinutes;
}
