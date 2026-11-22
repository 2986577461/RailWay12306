package com.xiaoyan.compute.instance;

import com.xiaoyan.compute.common.ApiResponse;
import com.xiaoyan.compute.config.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instances")
public class InstanceController {
    private final InstanceService instanceService;

    public InstanceController(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @GetMapping
    public ApiResponse<List<InstanceService.InstanceView>> list() {
        return ApiResponse.ok(instanceService.list(UserContext.userId()));
    }

    @GetMapping("/{instanceNo}")
    public ApiResponse<InstanceService.InstanceView> detail(@PathVariable String instanceNo) {
        return ApiResponse.ok(instanceService.detail(UserContext.userId(), instanceNo));
    }
}
