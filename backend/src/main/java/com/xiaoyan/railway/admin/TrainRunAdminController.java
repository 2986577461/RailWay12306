package com.xiaoyan.railway.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.basic.Train;
import com.xiaoyan.railway.basic.TrainMapper;
import com.xiaoyan.railway.basic.TrainRun;
import com.xiaoyan.railway.basic.TrainRunMapper;
import com.xiaoyan.railway.basic.dto.TrainRunCommand;
import com.xiaoyan.railway.common.ApiResponse;
import com.xiaoyan.railway.common.BizException;
import com.xiaoyan.railway.inventory.InventoryService;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
public class TrainRunAdminController {
    private final TrainRunMapper trainRunMapper;
    private final TrainMapper trainMapper;
    private final InventoryService inventoryService;

    public TrainRunAdminController(TrainRunMapper trainRunMapper, TrainMapper trainMapper,
                                   InventoryService inventoryService) {
        this.trainRunMapper = trainRunMapper;
        this.trainMapper = trainMapper;
        this.inventoryService = inventoryService;
    }

    /** Create a run for a train on a date, then seed its segment inventory (开售). */
    @PostMapping("/train-runs")
    public ApiResponse<Long> create(@Valid @RequestBody TrainRunCommand command) {
        Train train = trainMapper.selectById(command.trainId());
        if (train == null) {
            throw new BizException("车次不存在");
        }
        TrainRun run = TrainRun.builder()
                .trainId(command.trainId())
                .runDate(command.runDate())
                .saleStartAt(LocalDateTime.now())
                .status(1)
                .build();
        try {
            trainRunMapper.insert(run);
        } catch (DuplicateKeyException e) {
            throw new BizException("该车次当天已有运行");
        }
        inventoryService.seedRun(run, true);
        return ApiResponse.ok(run.getId());
    }

    /** Force-reset Redis segment inventory for all active runs (调库存/重开售). */
    @PostMapping("/inventory/reload")
    public ApiResponse<Void> reload() {
        inventoryService.reloadAll(true);
        return ApiResponse.ok(null);
    }
}
