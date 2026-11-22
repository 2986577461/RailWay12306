package com.xiaoyan.compute.billing;

import com.xiaoyan.compute.common.ApiResponse;
import com.xiaoyan.compute.config.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/summary")
    public ApiResponse<BillingService.BillingView> summary() {
        return ApiResponse.ok(billingService.summary(UserContext.userId()));
    }
}
