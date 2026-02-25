package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.UserQuota;
import com.toonflow.mapper.UserQuotaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserQuotaMapper userQuotaMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "toonflow:quota:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);

    /**
     * 检查并消耗配额。使用 Redis 分布式锁防止并发超额。
     *
     * @param userId 用户 ID
     * @param type   类型：chapter / image / video
     * @param count  消耗数量
     */
    public void checkAndConsume(Long userId, String type, int count) {
        if (userId == null) return;

        String lockKey = LOCK_PREFIX + userId;
        boolean locked = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL));

        if (!locked) {
            // 短暂等待后重试一次
            try { Thread.sleep(100); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            locked = Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL));
            if (!locked) {
                throw new BizException(ErrorCode.BIZ_ERROR, "操作过于频繁，请稍后重试");
            }
        }

        try {
            doCheckAndConsume(userId, type, count);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Transactional
    protected void doCheckAndConsume(Long userId, String type, int count) {
        UserQuota quota = userQuotaMapper.selectOne(
                new LambdaQueryWrapper<UserQuota>().eq(UserQuota::getUserId, userId));
        if (quota == null) return; // 无配额记录，不限制

        // 每日重置
        LocalDate today = LocalDate.now();
        if (!today.equals(quota.getResetDate())) {
            quota.setUsedChapters(0);
            quota.setUsedImages(0);
            quota.setUsedVideos(0);
            quota.setResetDate(today);
        }

        switch (type) {
            case "chapter" -> {
                int limit = quota.getDailyChapterLimit() != null ? quota.getDailyChapterLimit() : Integer.MAX_VALUE;
                int used = quota.getUsedChapters() != null ? quota.getUsedChapters() : 0;
                if (used + count > limit)
                    throw new BizException(ErrorCode.QUOTA_EXCEEDED, "章节生成配额已用完（今日剩余：" + (limit - used) + "）");
                quota.setUsedChapters(used + count);
            }
            case "image" -> {
                int limit = quota.getDailyImageLimit() != null ? quota.getDailyImageLimit() : Integer.MAX_VALUE;
                int used = quota.getUsedImages() != null ? quota.getUsedImages() : 0;
                if (used + count > limit)
                    throw new BizException(ErrorCode.QUOTA_EXCEEDED, "图片生成配额已用完（今日剩余：" + (limit - used) + "）");
                quota.setUsedImages(used + count);
            }
            case "video" -> {
                int limit = quota.getDailyVideoLimit() != null ? quota.getDailyVideoLimit() : Integer.MAX_VALUE;
                int used = quota.getUsedVideos() != null ? quota.getUsedVideos() : 0;
                if (used + count > limit)
                    throw new BizException(ErrorCode.QUOTA_EXCEEDED, "视频生成配额已用完（今日剩余：" + (limit - used) + "）");
                quota.setUsedVideos(used + count);
            }
            default -> log.warn("[Quota] Unknown type: {}", type);
        }

        userQuotaMapper.updateById(quota);
    }
}
