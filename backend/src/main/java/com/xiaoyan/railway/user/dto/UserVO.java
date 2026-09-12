package com.xiaoyan.railway.user.dto;

import java.time.LocalDateTime;

public record UserVO(Long id, String phone, String realName, LocalDateTime createdAt) { }
