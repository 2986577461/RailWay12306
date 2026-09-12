package com.xiaoyan.railway.passenger;

import com.xiaoyan.railway.common.ApiResponse;
import com.xiaoyan.railway.config.UserContext;
import com.xiaoyan.railway.passenger.dto.PassengerCommand;
import com.xiaoyan.railway.passenger.dto.PassengerVO;
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

/** All endpoints require login; the owner is the authenticated user. */
@RestController
@RequestMapping("/api/passengers")
public class PassengerController {
    private final PassengerService passengerService;

    public PassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    @PostMapping
    public ApiResponse<Void> add(@Valid @RequestBody PassengerCommand command) {
        passengerService.add(UserContext.userId(), command);
        return ApiResponse.ok(null);
    }

    @GetMapping
    public ApiResponse<List<PassengerVO>> list() {
        return ApiResponse.ok(passengerService.list(UserContext.userId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody PassengerCommand command) {
        passengerService.update(UserContext.userId(), id, command);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        passengerService.delete(UserContext.userId(), id);
        return ApiResponse.ok(null);
    }
}
