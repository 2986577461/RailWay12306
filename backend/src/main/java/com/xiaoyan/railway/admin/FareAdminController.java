package com.xiaoyan.railway.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.basic.Fare;
import com.xiaoyan.railway.basic.FareMapper;
import com.xiaoyan.railway.basic.dto.FareCommand;
import com.xiaoyan.railway.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/fares")
public class FareAdminController {
    private final FareMapper fareMapper;

    public FareAdminController(FareMapper fareMapper) {
        this.fareMapper = fareMapper;
    }

    @GetMapping
    public ApiResponse<List<Fare>> list() {
        return ApiResponse.ok(fareMapper.selectList(Wrappers.<Fare>lambdaQuery().orderByAsc(Fare::getId)));
    }

    /** Upsert by (train_id, seat_type_id, from_station_id, to_station_id). */
    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody FareCommand command) {
        Fare existing = fareMapper.selectOne(Wrappers.<Fare>lambdaQuery()
                .eq(Fare::getTrainId, command.trainId())
                .eq(Fare::getSeatTypeId, command.seatTypeId())
                .eq(Fare::getFromStationId, command.fromStationId())
                .eq(Fare::getToStationId, command.toStationId()));
        if (existing != null) {
            existing.setPrice(command.price());
            fareMapper.updateById(existing);
        } else {
            fareMapper.insert(Fare.builder()
                    .trainId(command.trainId())
                    .seatTypeId(command.seatTypeId())
                    .fromStationId(command.fromStationId())
                    .toStationId(command.toStationId())
                    .price(command.price())
                    .build());
        }
        return ApiResponse.ok(null);
    }
}
