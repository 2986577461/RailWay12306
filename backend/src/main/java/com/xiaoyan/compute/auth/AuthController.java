package com.xiaoyan.compute.auth;

import com.xiaoyan.compute.common.ApiResponse;
import com.xiaoyan.compute.config.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthService.AuthResult> register(@Valid @RequestBody RegisterCommand command) {
        return ApiResponse.ok(authService.register(command.email(), command.password(), command.displayName()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.AuthResult> login(@Valid @RequestBody LoginCommand command) {
        return ApiResponse.ok(authService.login(command.email(), command.password()));
    }

    @GetMapping("/me")
    public ApiResponse<AuthService.UserView> me() {
        return ApiResponse.ok(authService.me(UserContext.userId()));
    }

    public record RegisterCommand(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 64) String displayName
    ) { }

    public record LoginCommand(
            @NotBlank @Email String email,
            @NotBlank String password
    ) { }
}
