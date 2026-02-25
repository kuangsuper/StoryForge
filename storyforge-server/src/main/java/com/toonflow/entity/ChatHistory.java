package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_chat_history")
public class ChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String type;
    private String data;
    private String novel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
