package com.xiaoyan.compute.billing;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.compute.auth.AppUser;
import com.xiaoyan.compute.auth.UserMapper;
import com.xiaoyan.compute.catalog.GpuSku;
import com.xiaoyan.compute.catalog.GpuSkuMapper;
import com.xiaoyan.compute.order.OrderRepository;
import com.xiaoyan.compute.order.RentalOrder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BillingService {
    private final OrderRepository orderRepository;
    private final GpuSkuMapper skuMapper;
    private final UserMapper userMapper;
    private final BillingInvoiceMapper invoiceMapper;

    public BillingService(OrderRepository orderRepository, GpuSkuMapper skuMapper,
                          UserMapper userMapper, BillingInvoiceMapper invoiceMapper) {
        this.orderRepository = orderRepository;
        this.skuMapper = skuMapper;
        this.userMapper = userMapper;
        this.invoiceMapper = invoiceMapper;
    }

    public BillingView summary(Long userId) {
        AppUser user = userMapper.selectById(userId);
        List<RentalOrder> orders = orderRepository.listAllByUser(userId);
        BigDecimal monthlySpend = orders.stream().filter(this::billable)
                .map(RentalOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<GpuSku> skus = skuMapper.selectList(null);
        Map<Long, GpuSku> skuMap = skus.stream().collect(Collectors.toMap(GpuSku::getId, item -> item));
        List<UsageItem> usage = skus.stream().map(sku -> {
            List<RentalOrder> related = orders.stream().filter(o -> o.getSkuId().equals(sku.getId())).toList();
            int hours = related.stream().mapToInt(o -> o.getHours() * o.getQuantity()).sum();
            BigDecimal amount = related.stream().filter(this::billable)
                    .map(RentalOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new UsageItem(sku.getName(), hours, amount, sku.getAccent());
        }).filter(item -> item.hours() > 0).toList();
        List<InvoiceView> invoices = invoiceMapper.selectList(Wrappers.<BillingInvoice>lambdaQuery()
                        .eq(BillingInvoice::getUserId, userId)
                        .orderByDesc(BillingInvoice::getIssuedAt))
                .stream().map(i -> new InvoiceView(i.getInvoiceNo(), i.getPeriodLabel(), i.getAmount(),
                        i.getStatus(), i.getIssuedAt())).toList();
        BigDecimal budget = new BigDecimal("12500");
        return new BillingView(user == null ? BigDecimal.ZERO : user.getBalance(), monthlySpend,
                budget, new BigDecimal("1240"), usage, invoices);
    }

    private boolean billable(RentalOrder order) {
        return order.getStatus() != null && order.getStatus() != 1 && order.getStatus() != 6 && order.getStatus() != 9;
    }

    public record BillingView(
            BigDecimal balance,
            BigDecimal monthlySpend,
            BigDecimal budget,
            BigDecimal savings,
            List<UsageItem> usage,
            List<InvoiceView> invoices
    ) { }

    public record UsageItem(String name, int hours, BigDecimal amount, String color) { }
    public record InvoiceView(String invoiceNo, String periodLabel, BigDecimal amount, String status, java.time.LocalDate issuedAt) { }
}
