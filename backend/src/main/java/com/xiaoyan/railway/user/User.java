package com.xiaoyan.railway.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Entity mapped to the {@code user_account} table. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_account")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String phone;
    private String passwordHash;
    private String realName;
    private String idCardNo;
    private Integer idCardType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
