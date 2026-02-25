package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_role")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String role;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
