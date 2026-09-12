package com.xiaoyan.railway.passenger.dto;

import java.time.LocalDateTime;

public record PassengerVO(Long id, String name, String idCardNo, Integer passengerType,
                          String phone, LocalDateTime createdAt) { }
