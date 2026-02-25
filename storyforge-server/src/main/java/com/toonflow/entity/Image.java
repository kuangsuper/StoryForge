package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_image")
public class Image {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long assetsId;
    private Long scriptId;
    private String type;
    private String filePath;
    private Integer state;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
