package com.xiaoyan.compute.catalog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("gpu_inventory_slot")
public class GpuInventorySlot {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long skuId;
    private LocalDateTime slotStart;
    private Integer totalCount;
    private Integer lockedCount;
    private Integer soldCount;
    private Integer version;
}
