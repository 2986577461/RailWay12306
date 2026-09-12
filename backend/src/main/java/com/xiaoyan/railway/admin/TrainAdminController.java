package com.xiaoyan.railway.admin;

import com.xiaoyan.railway.basic.Train;
import com.xiaoyan.railway.basic.TrainService;
import com.xiaoyan.railway.basic.dto.TrainCommand;
import com.xiaoyan.railway.basic.dto.TrainDetailVO;
import com.xiaoyan.railway.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/trains")
public class TrainAdminController {
    private final TrainService trainService;

    public TrainAdminController(TrainService trainService) {
        this.trainService = trainService;
    }

    @GetMapping
    public ApiResponse<List<Train>> list() {
        return ApiResponse.ok(trainService.list());
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody TrainCommand command) {
        return ApiResponse.ok(trainService.create(command));
    }

    @GetMapping("/{id}")
    public ApiResponse<TrainDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(trainService.detail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody TrainCommand command) {
        trainService.update(id, command);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        trainService.delete(id);
        return ApiResponse.ok(null);
    }
}
