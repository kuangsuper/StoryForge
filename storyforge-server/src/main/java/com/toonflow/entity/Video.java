package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_video")
public class Video {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long scriptId;
    private String shotId;
    private Integer segmentId;
    private Long configId;
    private String prompt;
    private String videoPrompt;
    private String cameraMotion;
    private String filePath;
    private String lastFrame;
    private Integer state;
    private BigDecimal duration;
    private String resolution;
    private String manufacturer;
    private String model;
    private String taskId;
    private Integer retryCount;
    private Integer maxRetry;
    private Integer version;
    private Integer selected;
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
