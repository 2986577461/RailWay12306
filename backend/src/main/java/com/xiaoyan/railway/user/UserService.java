package com.xiaoyan.railway.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.common.BizException;
import com.xiaoyan.railway.security.JwtTokenProvider;
import com.xiaoyan.railway.user.dto.LoginByCodeCommand;
import com.xiaoyan.railway.user.dto.LoginCommand;
import com.xiaoyan.railway.user.dto.LoginResponse;
import com.xiaoyan.railway.user.dto.RegisterCommand;
import com.xiaoyan.railway.user.dto.UserVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final SmsCodeService smsCodeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserMapper userMapper, SmsCodeService smsCodeService,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.smsCodeService = smsCodeService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse register(RegisterCommand command) {
        if (!smsCodeService.verify(command.phone(), command.smsCode())) {
            throw new BizException("验证码错误或已过期");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .phone(command.phone())
                .passwordHash(passwordEncoder.encode(command.password()))
                .idCardType(1)
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException("手机号已注册");
        }
        return issueToken(user);
    }

    public LoginResponse login(LoginCommand command) {
        User user = findByPhone(command.phone());
        if (user == null || !passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new BizException("手机号或密码错误");
        }
        return issueToken(user);
    }

    public LoginResponse loginByCode(LoginByCodeCommand command) {
        if (!smsCodeService.verify(command.phone(), command.smsCode())) {
            throw new BizException("验证码错误或已过期");
        }
        User user = findByPhone(command.phone());
        if (user == null) {
            // SMS login auto-registers a new account (no password yet).
            // password_hash is NOT NULL, so store a BCrypt hash of an unguessable random value:
            // it satisfies the constraint while password login can never succeed for this account.
            LocalDateTime now = LocalDateTime.now();
            user = User.builder()
                    .phone(command.phone())
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .idCardType(1)
                    .status(1)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            userMapper.insert(user);
        }
        return issueToken(user);
    }

    public UserVO getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return toVO(user);
    }

    private User findByPhone(String phone) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
    }

    private LoginResponse issueToken(User user) {
        String token = jwtTokenProvider.generate(user.getId(), user.getPhone());
        return new LoginResponse(token, toVO(user));
    }

    private UserVO toVO(User user) {
        return new UserVO(user.getId(), user.getPhone(), user.getRealName(), user.getCreatedAt());
    }
}
