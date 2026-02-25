package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_model_map")
public class AiModelMap {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long configId;
    private String name;

    @TableField("`key`")
    private String key;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
