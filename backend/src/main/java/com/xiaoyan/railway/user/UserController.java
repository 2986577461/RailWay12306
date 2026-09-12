package com.xiaoyan.railway.user;

import com.xiaoyan.railway.common.ApiResponse;
import com.xiaoyan.railway.config.UserContext;
import com.xiaoyan.railway.user.dto.LoginByCodeCommand;
import com.xiaoyan.railway.user.dto.LoginCommand;
import com.xiaoyan.railway.user.dto.LoginResponse;
import com.xiaoyan.railway.user.dto.RegisterCommand;
import com.xiaoyan.railway.user.dto.SmsCodeCommand;
import com.xiaoyan.railway.user.dto.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final SmsCodeService smsCodeService;

    public UserController(UserService userService, SmsCodeService smsCodeService) {
        this.userService = userService;
        this.smsCodeService = smsCodeService;
    }

    /** Send a mock SMS code. Dev builds return the code in the response for easy testing. */
    @PostMapping("/sms-code")
    public ApiResponse<Map<String, String>> smsCode(@Valid @RequestBody SmsCodeCommand command) {
        if (!smsCodeService.canSend(command.phone())) {
            return ApiResponse.fail("发送太频繁，请稍后再试");
        }
        String code = smsCodeService.generateAndStore(command.phone());
        return ApiResponse.ok(Map.of("phone", command.phone(), "code", code));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterCommand command) {
        return ApiResponse.ok(userService.register(command));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginCommand command) {
        return ApiResponse.ok(userService.login(command));
    }

    @PostMapping("/login-by-code")
    public ApiResponse<LoginResponse> loginByCode(@Valid @RequestBody LoginByCodeCommand command) {
        return ApiResponse.ok(userService.loginByCode(command));
    }

    /** Requires login; the authenticated user is resolved from the JWT, not a header. */
    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.ok(userService.getById(UserContext.userId()));
    }
}
