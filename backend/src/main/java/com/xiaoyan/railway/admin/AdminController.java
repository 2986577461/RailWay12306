package com.xiaoyan.railway.admin;
import com.xiaoyan.railway.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @GetMapping("/overview")
    public ApiResponse<Map<String, String>> overview() { return ApiResponse.ok(Map.of("status", "UP")); }
}
