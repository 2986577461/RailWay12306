package com.xiaoyan.compute.dashboard;

import com.xiaoyan.compute.instance.InstanceService;
import com.xiaoyan.compute.order.OrderService;
import com.xiaoyan.compute.order.RentalOrder;
import com.xiaoyan.compute.order.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DashboardService {
    private final InstanceService instanceService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public DashboardService(InstanceService instanceService, OrderService orderService,
                            OrderRepository orderRepository) {
        this.instanceService = instanceService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    public DashboardView get(Long userId) {
        List<InstanceService.InstanceView> instances = instanceService.list(userId);
        List<OrderService.OrderListItemView> orders = orderService.list(userId);
        long active = instances.stream().filter(i -> "running".equals(i.status()) || "degraded".equals(i.status())).count();
        BigDecimal spend = orderRepository.listAllByUser(userId).stream()
                .filter(this::billable)
                .map(RentalOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int avgUtilization = instances.isEmpty() ? 0
                : (int) Math.round(instances.stream().mapToInt(InstanceService.InstanceView::utilization).average().orElse(0));
        return new DashboardView(
                new Metrics(active, spend, avgUtilization, new BigDecimal("99.98")),
                List.of(36, 42, 39, 51, 48, 57, 62, 59, 68, 64, 73, 78, 74, 83, 79, 88, 91, 86, 94, 89, 96, 92, 98, 94),
                instances,
                orders.stream().limit(5).toList(),
                List.of(
                        new Insight("H100 利用率偏高", "上海区域当前负载较高，建议将新任务调度到北京 A100。", "lime"),
                        new Insight("履约链路运行平稳", "过去 24 小时没有出现开通失败或退款异常。", "cyan"),
                        new Insight("可优化预留时长", "连续负载超过 8 小时时，72h 预留实例更划算。", "violet")
                )
        );
    }

    private boolean billable(RentalOrder order) {
        return order.getStatus() != null && order.getStatus() != 1 && order.getStatus() != 6 && order.getStatus() != 9;
    }

    public record DashboardView(
            Metrics metrics,
            List<Integer> usageSeries,
            List<InstanceService.InstanceView> instances,
            List<OrderService.OrderListItemView> recentOrders,
            List<Insight> insights
    ) { }

    public record Metrics(long activeInstances, BigDecimal monthlySpend, int avgUtilization, BigDecimal successRate) { }
    public record Insight(String title, String detail, String tone) { }
}
