package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_video_compose_config")
public class VideoComposeConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long scriptId;
    private String transition;
    private Integer transitionDuration;
    private String bgmPath;
    private Integer bgmVolume;
    private Integer ttsEnabled;
    private Integer ttsVolume;
    private Integer subtitleEnabled;
    private String subtitleStyle;
    private Integer watermarkEnabled;
    private String watermarkType;
    private String watermarkContent;
    private String watermarkPosition;
    private Integer watermarkOpacity;
    private Integer introEnabled;
    private String introText;
    private Integer introDuration;
    private Integer outroEnabled;
    private String outroText;
    private Integer outroDuration;
    private String outputResolution;
    private Integer outputFps;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
