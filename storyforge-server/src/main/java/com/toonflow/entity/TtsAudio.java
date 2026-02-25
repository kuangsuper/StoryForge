package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_tts_audio")
public class TtsAudio {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long scriptId;
    private String text;
    private String characterName;
    private String voiceId;
    private String filePath;
    private BigDecimal duration;
    private Integer state;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
