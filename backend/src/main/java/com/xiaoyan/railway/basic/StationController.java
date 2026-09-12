package com.xiaoyan.railway.basic;

import com.xiaoyan.railway.basic.dto.StationVO;
import com.xiaoyan.railway.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {
    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public ApiResponse<List<StationVO>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(stationService.list(keyword));
    }
}
