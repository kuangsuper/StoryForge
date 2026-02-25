package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.ai.model.AiRequest;
import com.toonflow.ai.model.ChatMessage;
import com.toonflow.ai.model.VideoRequest;
import com.toonflow.ai.model.VideoTaskResult;
import com.toonflow.ai.provider.TextAiProvider;
import com.toonflow.ai.provider.VideoAiProvider;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Video;
import com.toonflow.entity.VideoConfig;
import com.toonflow.mapper.VideoConfigMapper;
import com.toonflow.mapper.VideoMapper;
import com.toonflow.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoMapper videoMapper;
    private final VideoConfigMapper videoConfigMapper;
    private final AiProviderService aiProviderService;
    private final QuotaService quotaService;

    public Video submitGenerate(Long projectId, Long scriptId, String shotId,
                                Long configId, String prompt, String imagePath) {
        // 配额检查
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            quotaService.checkAndConsume(userId, "video", 1);
        } catch (BizException e) {
            throw e;
        } catch (Exception ignored) {
            // 非 HTTP 上下文跳过配额检查
        }
        Video video = new Video();
        video.setProjectId(projectId);
        video.setScriptId(scriptId);
        video.setShotId(shotId);
        video.setConfigId(configId);
        video.setPrompt(prompt);
        video.setState(0);
        video.setRetryCount(0);
        video.setMaxRetry(3);
        video.setVersion(1);
        video.setSelected(1);

        if (configId != null) {
            VideoConfig config = videoConfigMapper.selectById(configId);
            if (config != null) {
                video.setManufacturer(config.getManufacturer());
                video.setResolution(config.getResolution() != null ? config.getResolution() : "1080p");
            }
        }
        if (video.getResolution() == null) video.setResolution("1080p");

        // 首尾帧衔接：查询同 scriptId 前一个已完成视频的 lastFrame
        String firstFrame = imagePath;
        if (imagePath == null && scriptId != null && shotId != null) {
            Video prev = findPrevCompleted(scriptId, shotId);
            if (prev != null && prev.getLastFrame() != null) {
                firstFrame = prev.getLastFrame();
            }
        }

        videoMapper.insert(video);
        generateVideoAsync(video.getId(), prompt, firstFrame, video.getResolution());
        return video;
    }

    @Async
    public void generateVideoAsync(Long videoId, String prompt, String imagePath, String resolution) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) return;

        try {
            VideoAiProvider provider = aiProviderService.getVideoProvider("videoGenerate");

            VideoRequest request = VideoRequest.builder()
                    .prompt(prompt)
                    .mode(imagePath != null ? "singleImage" : "text")
                    .imageUrls(imagePath != null ? List.of(imagePath) : null)
                    .resolution(resolution)
                    .build();

            String taskId = provider.createTask(request);
            video.setTaskId(taskId);
            videoMapper.updateById(video);

            VideoTaskResult result = pollUntilComplete(provider, taskId);

            if ("completed".equals(result.getState())) {
                video.setFilePath(result.getVideoUrl());
                video.setLastFrame(result.getLastFrameUrl());
                video.setState(1);
                if (result.getDuration() != null) {
                    video.setDuration(BigDecimal.valueOf(result.getDuration()));
                }
                videoMapper.updateById(video);
            } else {
                handleFailure(video, result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Video generation failed for videoId={}", videoId, e);
            handleFailure(video, e.getMessage());
        }
    }

    private VideoTaskResult pollUntilComplete(VideoAiProvider provider, String taskId) {
        int maxPolls = 500;
        for (int i = 0; i < maxPolls; i++) {
            VideoTaskResult result = provider.pollTask(taskId);
            if ("completed".equals(result.getState()) || "failed".equals(result.getState())) {
                return result;
            }
            try { Thread.sleep(2000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return VideoTaskResult.builder().state("failed").errorMessage("轮询被中断").build();
            }
        }
        return VideoTaskResult.builder().state("failed").errorMessage("轮询超时").build();
    }

    private void handleFailure(Video video, String errorMessage) {
        video.setErrorMessage(errorMessage);
        int nextRetry = (video.getRetryCount() != null ? video.getRetryCount() : 0) + 1;
        int maxRetry = video.getMaxRetry() != null ? video.getMaxRetry() : 3;

        if (nextRetry <= maxRetry) {
            video.setRetryCount(nextRetry);
            videoMapper.updateById(video);

            String retryPrompt = video.getPrompt();
            String retryResolution = video.getResolution();

            if (nextRetry == 2) {
                // 第2次：AI 润色 prompt
                retryPrompt = polishPrompt(video.getPrompt());
                log.info("[Video] Retry #{} with polished prompt for videoId={}", nextRetry, video.getId());
            } else if (nextRetry == 3) {
                // 第3次：降分辨率
                retryResolution = downgradeResolution(video.getResolution());
                log.info("[Video] Retry #{} with downgraded resolution {} for videoId={}", nextRetry, retryResolution, video.getId());
            }

            generateVideoAsync(video.getId(), retryPrompt, null, retryResolution);
        } else {
            video.setState(-1);
            videoMapper.updateById(video);
        }
    }

    private String polishPrompt(String original) {
        if (original == null || original.isBlank()) return original;
        try {
            TextAiProvider provider = aiProviderService.getTextProvider("videoPromptPolish");
            AiRequest request = AiRequest.builder()
                    .systemPrompt("You are a video prompt optimizer. Improve the following video generation prompt to be more detailed, cinematic, and visually descriptive. Keep it under 200 words. Output only the improved prompt.")
                    .messages(List.of(ChatMessage.user(original)))
                    .build();
            String polished = provider.invoke(request);
            return polished != null && !polished.isBlank() ? polished : original;
        } catch (Exception e) {
            log.warn("[Video] Failed to polish prompt: {}", e.getMessage());
            return original;
        }
    }

    private String downgradeResolution(String resolution) {
        if (resolution == null) return "720p";
        return switch (resolution) {
            case "4K", "2160p" -> "1080p";
            case "1080p" -> "720p";
            case "720p" -> "480p";
            default -> resolution;
        };
    }

    private Video findPrevCompleted(Long scriptId, String currentShotId) {
        try {
            List<Video> completed = videoMapper.selectList(
                    new LambdaQueryWrapper<Video>()
                            .eq(Video::getScriptId, scriptId)
                            .eq(Video::getState, 1)
                            .eq(Video::getSelected, 1)
                            .isNotNull(Video::getLastFrame)
                            .orderByAsc(Video::getShotId));
            // 找到 shotId 排序在当前之前的最后一个
            Video prev = null;
            for (Video v : completed) {
                if (v.getShotId() != null && v.getShotId().compareTo(currentShotId) < 0) {
                    prev = v;
                }
            }
            return prev;
        } catch (Exception e) {
            log.warn("[Video] Failed to find prev completed video: {}", e.getMessage());
            return null;
        }
    }

    public void retryVideo(Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (video.getRetryCount() >= video.getMaxRetry()) {
            throw new BizException(ErrorCode.BIZ_ERROR, "已达最大重试次数");
        }
        video.setRetryCount(video.getRetryCount() + 1);
        video.setState(0);
        video.setErrorMessage(null);
        videoMapper.updateById(video);
        generateVideoAsync(video.getId(), video.getPrompt(), null, video.getResolution());
    }

    public void batchRetry(Long projectId, Long scriptId) {
        List<Video> failed = videoMapper.selectList(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getProjectId, projectId)
                        .eq(Video::getScriptId, scriptId)
                        .eq(Video::getState, -1));
        for (Video v : failed) {
            if (v.getRetryCount() < v.getMaxRetry()) {
                v.setRetryCount(v.getRetryCount() + 1);
                v.setState(0);
                v.setErrorMessage(null);
                videoMapper.updateById(v);
                generateVideoAsync(v.getId(), v.getPrompt(), null, v.getResolution());
            }
        }
    }

    public void selectVersion(Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) throw new BizException(ErrorCode.NOT_FOUND);
        List<Video> versions = videoMapper.selectList(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getShotId, video.getShotId())
                        .eq(Video::getScriptId, video.getScriptId()));
        for (Video v : versions) {
            v.setSelected(v.getId().equals(videoId) ? 1 : 0);
            videoMapper.updateById(v);
        }
    }

    public Video regenerate(Long videoId) {
        Video original = videoMapper.selectById(videoId);
        if (original == null) throw new BizException(ErrorCode.NOT_FOUND);
        original.setSelected(0);
        videoMapper.updateById(original);

        Video newVideo = new Video();
        newVideo.setProjectId(original.getProjectId());
        newVideo.setScriptId(original.getScriptId());
        newVideo.setShotId(original.getShotId());
        newVideo.setSegmentId(original.getSegmentId());
        newVideo.setConfigId(original.getConfigId());
        newVideo.setPrompt(original.getPrompt());
        newVideo.setState(0);
        newVideo.setRetryCount(0);
        newVideo.setMaxRetry(3);
        newVideo.setVersion(original.getVersion() + 1);
        newVideo.setSelected(1);
        newVideo.setManufacturer(original.getManufacturer());
        newVideo.setResolution(original.getResolution());
        videoMapper.insert(newVideo);

        generateVideoAsync(newVideo.getId(), newVideo.getPrompt(), null, newVideo.getResolution());
        return newVideo;
    }

    public List<Video> getVersions(String shotId) {
        return videoMapper.selectList(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getShotId, shotId)
                        .orderByAsc(Video::getVersion));
    }

    public List<Video> listByScript(Long scriptId) {
        return videoMapper.selectList(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getScriptId, scriptId)
                        .orderByAsc(Video::getShotId));
    }

    public long countPending(Long projectId) {
        return videoMapper.selectCount(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getProjectId, projectId)
                        .eq(Video::getState, 0));
    }
}
