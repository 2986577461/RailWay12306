package com.xiaoyan.railway.basic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entity mapped to the {@code station} table. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("station")
public class Station {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String stationCode;
    private String stationName;
    private String cityName;
    private String pinyin;
    private Integer status;
}
