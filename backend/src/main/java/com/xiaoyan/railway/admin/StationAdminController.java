package com.xiaoyan.railway.admin;

import com.xiaoyan.railway.basic.StationService;
import com.xiaoyan.railway.basic.dto.StationCommand;
import com.xiaoyan.railway.basic.dto.StationVO;
import com.xiaoyan.railway.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stations")
public class StationAdminController {
    private final StationService stationService;

    public StationAdminController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public ApiResponse<List<StationVO>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(stationService.list(keyword));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody StationCommand command) {
        stationService.create(command);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody StationCommand command) {
        stationService.update(id, command);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        stationService.delete(id);
        return ApiResponse.ok(null);
    }
}
