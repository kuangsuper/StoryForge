package com.toonflow.service.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的流水线状态存储，替代内存 ConcurrentHashMap。
 * 服务重启后状态不丢失。
 *
 * Redis key 结构：
 *   toonflow:pipeline:{projectId}:state       → 当前 PipelineState 名称
 *   toonflow:pipeline:{projectId}:failedStep  → 失败时的步骤状态名称
 *
 * 所有 key 设置 2 小时 TTL，流水线完成或终止时主动清理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineStateStore {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "toonflow:pipeline:";
    private static final Duration TTL = Duration.ofHours(2);

    // ---- State Machine ----

    public void saveState(Long projectId, PipelineState state) {
        String key = stateKey(projectId);
        redisTemplate.opsForValue().set(key, state.name(), TTL);
    }

    public PipelineState getState(Long projectId) {
        String val = redisTemplate.opsForValue().get(stateKey(projectId));
        if (val == null) return null;
        try {
            return PipelineState.valueOf(val);
        } catch (IllegalArgumentException e) {
            log.warn("[PipelineStateStore] Unknown state '{}' for project={}", val, projectId);
            return null;
        }
    }

    // ---- Failed Step ----

    public void saveFailedStep(Long projectId, PipelineState failedStep) {
        String key = failedStepKey(projectId);
        redisTemplate.opsForValue().set(key, failedStep.name(), TTL);
    }

    public PipelineState getFailedStep(Long projectId) {
        String val = redisTemplate.opsForValue().get(failedStepKey(projectId));
        if (val == null) return null;
        try {
            return PipelineState.valueOf(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---- Cleanup ----

    public void remove(Long projectId) {
        redisTemplate.delete(stateKey(projectId));
        redisTemplate.delete(failedStepKey(projectId));
    }

    public boolean exists(Long projectId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(stateKey(projectId)));
    }

    // ---- Key builders ----

    private String stateKey(Long projectId) {
        return PREFIX + projectId + ":state";
    }

    private String failedStepKey(Long projectId) {
        return PREFIX + projectId + ":failedStep";
    }
}
