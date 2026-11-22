package com.xiaoyan.compute.billing;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("billing_invoice")
public class BillingInvoice {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long userId;
    private String invoiceNo;
    private String periodLabel;
    private BigDecimal amount;
    private String status;
    private LocalDate issuedAt;
}
