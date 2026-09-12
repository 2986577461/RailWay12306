package com.xiaoyan.railway.passenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PassengerCommand(
        @NotBlank(message = "乘车人姓名不能为空")
        String name,

        @NotBlank(message = "身份证号不能为空")
        @Pattern(regexp = "^(\\d{15}|\\d{17}[\\dXx])$", message = "身份证号格式不正确")
        String idCardNo,

        Integer passengerType,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone
) { }
