package com.xiaoyan.compute.catalog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("gpu_sku")
public class GpuSku {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String code;
    private String name;
    private String architecture;
    private Integer memoryGb;
    private String memoryType;
    private String bandwidth;
    private String tflops;
    private BigDecimal pricePerHour;
    private String region;
    private String badge;
    private String description;
    private String useCases;
    private String accent;
    private Integer capacity;
    private Integer status;
}
