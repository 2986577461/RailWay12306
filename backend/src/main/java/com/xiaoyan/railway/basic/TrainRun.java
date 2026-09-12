package com.xiaoyan.railway.basic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Entity mapped to the {@code train_run} table (a train running on a specific date). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("train_run")
public class TrainRun {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long trainId;
    private LocalDate runDate;
    private LocalDateTime saleStartAt;
    private Integer status;
}
