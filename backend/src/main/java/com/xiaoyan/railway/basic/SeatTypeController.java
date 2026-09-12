package com.xiaoyan.railway.basic;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seat-types")
public class SeatTypeController {
    private final SeatTypeMapper seatTypeMapper;

    public SeatTypeController(SeatTypeMapper seatTypeMapper) {
        this.seatTypeMapper = seatTypeMapper;
    }

    @GetMapping
    public ApiResponse<List<SeatType>> list() {
        return ApiResponse.ok(seatTypeMapper.selectList(Wrappers.<SeatType>lambdaQuery().orderByAsc(SeatType::getId)));
    }
}
