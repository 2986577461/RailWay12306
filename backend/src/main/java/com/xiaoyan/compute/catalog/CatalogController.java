package com.xiaoyan.compute.catalog;

import com.xiaoyan.compute.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/gpus")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ApiResponse<List<CatalogService.GpuView>> list() {
        return ApiResponse.ok(catalogService.listActive());
    }

    @GetMapping("/{code}")
    public ApiResponse<CatalogService.GpuView> detail(@PathVariable String code) {
        return ApiResponse.ok(catalogService.detail(code));
    }

    @GetMapping("/{code}/availability")
    public ApiResponse<CatalogService.AvailabilityView> availability(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(defaultValue = "1") int hours) {
        if (hours < 1 || hours > 72) {
            return ApiResponse.fail("租用时长需在 1 到 72 小时之间");
        }
        return ApiResponse.ok(catalogService.availability(code, startAt, hours));
    }
}
