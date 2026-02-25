package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_user_quota")
public class UserQuota {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer dailyChapterLimit;
    private Integer dailyImageLimit;
    private Integer dailyVideoLimit;
    private Integer usedChapters;
    private Integer usedImages;
    private Integer usedVideos;
    private LocalDate resetDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
