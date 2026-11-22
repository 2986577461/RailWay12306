package com.xiaoyan.compute.auth;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.compute.common.BizException;
import com.xiaoyan.compute.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResult register(String email, String password, String displayName) {
        String normalized = normalizeEmail(email);
        Long count = userMapper.selectCount(Wrappers.<AppUser>lambdaQuery().eq(AppUser::getEmail, normalized));
        if (count != null && count > 0) {
            throw new BizException("该邮箱已注册");
        }
        LocalDateTime now = LocalDateTime.now();
        AppUser user = AppUser.builder()
                .id(IdWorker.getId())
                .email(normalized)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName == null || displayName.isBlank() ? normalized.substring(0, normalized.indexOf('@')) : displayName.trim())
                .status(1)
                .balance(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(user);
        return result(user);
    }

    public AuthResult login(String email, String password) {
        AppUser user = userMapper.selectOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getEmail, normalizeEmail(email)));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("邮箱或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException("邮箱或密码错误");
        }
        return result(user);
    }

    public UserView me(Long userId) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return new UserView(user.getId(), user.getEmail(), user.getDisplayName(), user.getBalance());
    }

    private AuthResult result(AppUser user) {
        return new AuthResult(tokenProvider.createToken(user.getId(), user.getEmail()),
                new UserView(user.getId(), user.getEmail(), user.getDisplayName(), user.getBalance()));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record AuthResult(String token, UserView user) { }
    public record UserView(Long id, String email, String displayName, BigDecimal balance) { }
}
