package com.xiaoyan.compute.catalog;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.compute.common.BizException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class CatalogService {
    private final GpuSkuMapper skuMapper;
    private final GpuInventorySlotMapper slotMapper;

    public CatalogService(GpuSkuMapper skuMapper, GpuInventorySlotMapper slotMapper) {
        this.skuMapper = skuMapper;
        this.slotMapper = slotMapper;
    }

    public List<GpuView> listActive() {
        return skuMapper.selectList(Wrappers.<GpuSku>lambdaQuery()
                        .eq(GpuSku::getStatus, 1)
                        .orderByAsc(GpuSku::getId))
                .stream().map(this::toView).toList();
    }

    public GpuView detail(String code) {
        return toView(findSku(code));
    }

    public GpuSku findSku(String code) {
        GpuSku sku = skuMapper.selectOne(Wrappers.<GpuSku>lambdaQuery()
                .eq(GpuSku::getCode, code)
                .eq(GpuSku::getStatus, 1));
        if (sku == null) {
            throw new BizException("GPU 规格不存在");
        }
        return sku;
    }

    public AvailabilityView availability(String code, LocalDateTime startAt, int hours) {
        GpuSku sku = findSku(code);
        LocalDateTime normalized = normalizeStart(startAt);
        List<GpuInventorySlot> slots = slots(sku.getId(), normalized, hours);
        if (slots.size() != hours) {
            throw new BizException("该租期暂未开放");
        }
        int available = slots.stream()
                .mapToInt(slot -> Math.max(0, slot.getTotalCount() - slot.getLockedCount() - slot.getSoldCount()))
                .min().orElse(0);
        return new AvailabilityView(code, normalized, hours, available,
                slots.get(0).getTotalCount(), sku.getPricePerHour(),
                sku.getPricePerHour().multiply(BigDecimal.valueOf((long) hours)));
    }

    public List<GpuInventorySlot> slots(Long skuId, LocalDateTime startAt, int hours) {
        return slotMapper.selectList(Wrappers.<GpuInventorySlot>lambdaQuery()
                .eq(GpuInventorySlot::getSkuId, skuId)
                .ge(GpuInventorySlot::getSlotStart, startAt)
                .lt(GpuInventorySlot::getSlotStart, startAt.plusHours(hours))
                .orderByAsc(GpuInventorySlot::getSlotStart));
    }

    public LocalDateTime normalizeStart(LocalDateTime startAt) {
        LocalDateTime value = startAt == null ? LocalDateTime.now() : startAt;
        return value.truncatedTo(ChronoUnit.HOURS);
    }

    private GpuView toView(GpuSku sku) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        List<GpuInventorySlot> slots = slotMapper.selectList(Wrappers.<GpuInventorySlot>lambdaQuery()
                .eq(GpuInventorySlot::getSkuId, sku.getId())
                .ge(GpuInventorySlot::getSlotStart, now)
                .lt(GpuInventorySlot::getSlotStart, now.plusHours(24))
                .orderByAsc(GpuInventorySlot::getSlotStart));
        int available = slots.stream()
                .mapToInt(slot -> Math.max(0, slot.getTotalCount() - slot.getLockedCount() - slot.getSoldCount()))
                .min().orElse(0);
        int total = slots.isEmpty() ? sku.getCapacity() : slots.get(0).getTotalCount();
        List<String> useCases = sku.getUseCases() == null || sku.getUseCases().isBlank()
                ? List.of()
                : Arrays.stream(sku.getUseCases().split(",")).map(String::trim).toList();
        return new GpuView(sku.getCode(), sku.getId(), sku.getName(), sku.getArchitecture(),
                sku.getMemoryGb() + " GB", sku.getMemoryType(), sku.getBandwidth(), sku.getTflops(),
                sku.getPricePerHour(), available, total, sku.getRegion(), sku.getBadge(),
                sku.getAccent(), sku.getDescription(), useCases);
    }

    public record GpuView(
            String id,
            Long skuId,
            String name,
            String architecture,
            String memory,
            String memoryType,
            String bandwidth,
            String tflops,
            BigDecimal pricePerHour,
            int available,
            int total,
            String region,
            String badge,
            String accent,
            String description,
            List<String> useCases
    ) { }

    public record AvailabilityView(
            String gpuId,
            LocalDateTime startAt,
            int hours,
            int available,
            int total,
            BigDecimal pricePerHour,
            BigDecimal estimatedTotal
    ) { }
}
