package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.entity.*;
import com.toonflow.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectMapper projectMapper;
    private final NovelMapper novelMapper;
    private final VideoMapper videoMapper;
    private final VideoComposeMapper videoComposeMapper;
    private final ImageMapper imageMapper;
    private final AgentLogMapper agentLogMapper;
    private final TaskListMapper taskListMapper;

    public Map<String, Object> getStats() {
        return Map.of(
                "totalProjects", projectMapper.selectCount(null),
                "totalChapters", novelMapper.selectCount(null),
                "totalVideos", videoMapper.selectCount(null),
                "totalComposedVideos", videoComposeMapper.selectCount(null),
                "totalImages", imageMapper.selectCount(null)
        );
    }

    public Map<String, Object> getDailyMetrics() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return Map.of(
                "chaptersToday", novelMapper.selectCount(
                        new LambdaQueryWrapper<Novel>().ge(Novel::getCreateTime, todayStart)),
                "imagesToday", imageMapper.selectCount(
                        new LambdaQueryWrapper<Image>().ge(Image::getCreateTime, todayStart)),
                "videosToday", videoMapper.selectCount(
                        new LambdaQueryWrapper<Video>().ge(Video::getCreateTime, todayStart))
        );
    }

    public Map<String, Object> getModelUsage() {
        long totalCalls = agentLogMapper.selectCount(
                new LambdaQueryWrapper<AgentLog>().eq(AgentLog::getAction, "llmCall"));
        long failedCalls = agentLogMapper.selectCount(
                new LambdaQueryWrapper<AgentLog>()
                        .eq(AgentLog::getAction, "llmCall")
                        .eq(AgentLog::getStatus, "failed"));

        // 按 agentType 分组统计，供前端 Dashboard 展示
        List<AgentLog> allLlmLogs = agentLogMapper.selectList(
                new LambdaQueryWrapper<AgentLog>().eq(AgentLog::getAction, "llmCall")
                        .select(AgentLog::getAgentType, AgentLog::getStatus));

        Map<String, long[]> modelMap = new java.util.HashMap<>();
        for (AgentLog log : allLlmLogs) {
            String model = log.getAgentType() != null ? log.getAgentType() : "unknown";
            long[] counts = modelMap.computeIfAbsent(model, k -> new long[]{0, 0});
            counts[0]++;
            if ("failed".equals(log.getStatus())) counts[1]++;
        }

        List<Map<String, Object>> models = modelMap.entrySet().stream().map(e -> {
            long total = e.getValue()[0];
            long failed = e.getValue()[1];
            return Map.<String, Object>of(
                    "name", e.getKey(),
                    "totalCalls", total,
                    "failedCalls", failed,
                    "errorRate", total > 0 ? (double) failed / total : 0.0
            );
        }).toList();

        return Map.of(
                "totalCalls", totalCalls,
                "failedCalls", failedCalls,
                "errorRate", totalCalls > 0 ? (double) failedCalls / totalCalls : 0.0,
                "models", models
        );
    }

    public Map<String, Object> getPipelineStats() {
        long total = taskListMapper.selectCount(null);
        long completed = taskListMapper.selectCount(
                new LambdaQueryWrapper<TaskList>().eq(TaskList::getState, "ALL_COMPLETE"));
        long failed = taskListMapper.selectCount(
                new LambdaQueryWrapper<TaskList>().eq(TaskList::getState, "STEP_FAILED"));
        return Map.of("total", total, "completed", completed, "failed", failed);
    }
}
