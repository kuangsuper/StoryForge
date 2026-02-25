package com.toonflow.agent.core;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisAgentLock {

    private final StringRedisTemplate redisTemplate;
    private static final long LOCK_TIMEOUT_MINUTES = 30;

    public boolean tryLock(Long projectId) {
        String key = lockKey(projectId);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, Thread.currentThread().getName(),
                        Duration.ofMinutes(LOCK_TIMEOUT_MINUTES));
        return Boolean.TRUE.equals(success);
    }

    public void unlock(Long projectId) {
        redisTemplate.delete(lockKey(projectId));
    }

    public boolean isLocked(Long projectId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(projectId)));
    }

    private String lockKey(Long projectId) {
        return "toonflow:agent:lock:" + projectId;
    }
}
