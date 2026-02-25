package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Video;
import com.toonflow.entity.VideoCompose;
import com.toonflow.entity.VideoComposeConfig;
import com.toonflow.mapper.VideoComposeConfigMapper;
import com.toonflow.mapper.VideoComposeMapper;
import com.toonflow.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoComposeService {

    private final VideoComposeMapper videoComposeMapper;
    private final VideoComposeConfigMapper videoComposeConfigMapper;
    private final VideoMapper videoMapper;
    private final ObjectMapper objectMapper;

    @Value("${toonflow.python-service.base-url:http://localhost:8001}")
    private String pythonServiceUrl;

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient SHARED_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build();

    public VideoCompose submit(Long projectId, Long scriptId, Long configId) {
        List<Video> videos = videoMapper.selectList(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getProjectId, projectId)
                        .eq(Video::getScriptId, scriptId)
                        .eq(Video::getSelected, 1)
                        .eq(Video::getState, 1)
                        .orderByAsc(Video::getShotId));

        if (videos.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "没有可合成的视频片段");
        }

        String videoIds = videos.stream()
                .map(v -> String.valueOf(v.getId()))
                .collect(Collectors.joining(","));

        VideoCompose compose = new VideoCompose();
        compose.setProjectId(projectId);
        compose.setScriptId(scriptId);
        compose.setConfigId(configId);
        compose.setVideoIds(videoIds);
        compose.setState(0);
        compose.setRetryCount(0);
        videoComposeMapper.insert(compose);

        composeAsync(compose.getId());
        return compose;
    }

    @Async
    public void composeAsync(Long composeId) {
        VideoCompose compose = videoComposeMapper.selectById(composeId);
        if (compose == null) return;

        try {
            // 获取视频列表
            List<Long> videoIdList = Arrays.stream(compose.getVideoIds().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::parseLong).collect(Collectors.toList());

            List<Video> videos = videoMapper.selectBatchIds(videoIdList);
            List<String> videoPaths = videos.stream()
                    .map(Video::getFilePath).filter(Objects::nonNull).collect(Collectors.toList());

            // 获取合成配置
            VideoComposeConfig config = compose.getConfigId() != null
                    ? videoComposeConfigMapper.selectById(compose.getConfigId()) : null;

            // 构建请求体
            Map<String, Object> body = new HashMap<>();
            body.put("compose_id", composeId);
            body.put("video_paths", videoPaths);
            body.put("video_ids", videoIdList);
            if (config != null) {
                body.put("resolution", config.getOutputResolution());
                body.put("fps", config.getOutputFps());
                body.put("bgm_path", config.getBgmPath());
                body.put("bgm_volume", config.getBgmVolume());
                body.put("subtitle_enabled", config.getSubtitleEnabled());
            }

            String json = objectMapper.writeValueAsString(body);

            Request request = new Request.Builder()
                    .url(pythonServiceUrl + "/compose")
                    .post(RequestBody.create(json, JSON_TYPE))
                    .build();

            try (Response response = SHARED_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Python服务返回错误: " + response.code());
                }
                String respBody = response.body() != null ? response.body().string() : "{}";
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(respBody, Map.class);
                String outputPath = (String) result.get("output_path");

                compose.setState(1);
                compose.setFilePath(outputPath != null ? outputPath : "/compose/" + composeId + "/output.mp4");
                videoComposeMapper.updateById(compose);
                log.info("[VideoCompose] Compose completed for composeId={}, path={}", composeId, outputPath);
            }

        } catch (Exception e) {
            log.error("[VideoCompose] Compose failed for composeId={}: {}", composeId, e.getMessage());
            compose.setState(-1);
            compose.setErrorMessage(e.getMessage());
            compose.setRetryCount(compose.getRetryCount() + 1);
            videoComposeMapper.updateById(compose);
        }
    }

    /**
     * 从剧本内容生成 SRT 字幕
     */
    public String generateSrt(String scriptContent) {
        if (scriptContent == null || scriptContent.isBlank()) return "";
        StringBuilder srt = new StringBuilder();
        // 按行提取对话（格式：角色名：台词）
        String[] lines = scriptContent.split("\n");
        int index = 1;
        int startSec = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // 匹配 "角色：台词" 或 "【旁白】台词" 格式
            String dialogue = null;
            if (line.contains("：") || line.contains(":")) {
                int colonIdx = line.indexOf('：');
                if (colonIdx < 0) colonIdx = line.indexOf(':');
                if (colonIdx > 0 && colonIdx < line.length() - 1) {
                    dialogue = line.substring(colonIdx + 1).trim();
                }
            } else if (line.startsWith("【") && line.contains("】")) {
                int end = line.indexOf('】');
                dialogue = line.substring(end + 1).trim();
            }
            if (dialogue == null || dialogue.isEmpty()) continue;

            // 估算时长：每字约0.3秒，最少2秒
            int duration = Math.max(2, (int) (dialogue.length() * 0.3));
            int endSec = startSec + duration;

            srt.append(index++).append("\n");
            srt.append(formatSrtTime(startSec)).append(" --> ").append(formatSrtTime(endSec)).append("\n");
            srt.append(dialogue).append("\n\n");
            startSec = endSec + 1;
        }
        return srt.toString();
    }

    private String formatSrtTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d,000", h, m, s);
    }

    public VideoCompose getById(Long composeId) {
        VideoCompose compose = videoComposeMapper.selectById(composeId);
        if (compose == null) throw new BizException(ErrorCode.NOT_FOUND);
        return compose;
    }

    public List<VideoCompose> listByScript(Long projectId, Long scriptId) {
        return videoComposeMapper.selectList(
                new LambdaQueryWrapper<VideoCompose>()
                        .eq(VideoCompose::getProjectId, projectId)
                        .eq(VideoCompose::getScriptId, scriptId)
                        .orderByDesc(VideoCompose::getCreateTime));
    }

    public void retry(Long composeId) {
        VideoCompose compose = videoComposeMapper.selectById(composeId);
        if (compose == null) throw new BizException(ErrorCode.NOT_FOUND);
        compose.setState(0);
        compose.setErrorMessage(null);
        videoComposeMapper.updateById(compose);
        composeAsync(composeId);
    }
}
