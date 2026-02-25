package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_material")
public class Material {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private String filePath;
    private String category;
    private BigDecimal duration;
    private String tags;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
