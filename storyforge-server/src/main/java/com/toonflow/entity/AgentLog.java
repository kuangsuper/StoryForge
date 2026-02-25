package com.toonflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_agent_log")
public class AgentLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String agentType;
    private String sessionId;
    private Long parentLogId;
    private String action;
    private String agentName;
    private String toolName;
    private String input;
    private String output;
    private Long duration;
    private Integer tokenUsed;
    private String status;
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
