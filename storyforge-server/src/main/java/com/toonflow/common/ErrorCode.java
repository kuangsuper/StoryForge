package com.toonflow.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    QUOTA_EXCEEDED(429, "配额已用完"),
    BIZ_ERROR(500, "业务异常"),
    AI_CALL_FAILED(510, "AI调用失败"),
    AI_TIMEOUT(511, "AI调用超时"),
    AGENT_BUSY(512, "Agent正在执行中，请稍后再试"),
    AGENT_LOCK_FAILED(513, "当前项目有其他Agent正在执行写操作");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
