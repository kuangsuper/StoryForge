package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.agent.storyboard.Segment;
import com.toonflow.agent.storyboard.Shot;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.ChatHistory;
import com.toonflow.entity.Script;
import com.toonflow.mapper.ChatHistoryMapper;
import com.toonflow.mapper.ScriptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryboardService {

    private final ChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;
    private final ScriptMapper scriptMapper;

    public void save(Long projectId, Long scriptId, List<Segment> segments, List<Shot> shots) {
        String type = "storyboard_" + scriptId;
        Map<String, Object> data = Map.of("segments", segments, "shots", shots, "confirmed", false);
        saveData(projectId, type, toJson(data));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getByScript(Long projectId, Long scriptId) {
        String type = "storyboard_" + scriptId;
        ChatHistory record = chatHistoryMapper.selectOne(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getProjectId, projectId)
                        .eq(ChatHistory::getType, type));
        if (record == null || record.getData() == null) {
            return Map.of("segments", Collections.emptyList(), "shots", Collections.emptyList(), "confirmed", false);
        }
        try {
            return objectMapper.readValue(record.getData(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("segments", Collections.emptyList(), "shots", Collections.emptyList(), "confirmed", false);
        }
    }

    public void confirm(Long projectId, Long scriptId) {
        Map<String, Object> existing = getByScript(projectId, scriptId);
        Map<String, Object> data = new java.util.HashMap<>(existing);
        data.put("confirmed", true);
        saveData(projectId, "storyboard_" + scriptId, toJson(data));
    }

    private void saveData(Long projectId, String type, String json) {
        ChatHistory existing = chatHistoryMapper.selectOne(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getProjectId, projectId)
                        .eq(ChatHistory::getType, type));
        if (existing != null) {
            existing.setData(json);
            chatHistoryMapper.updateById(existing);
        } else {
            ChatHistory record = new ChatHistory();
            record.setProjectId(projectId);
            record.setType(type);
            record.setData(json);
            chatHistoryMapper.insert(record);
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { throw new BizException(ErrorCode.BIZ_ERROR, "JSON序列化失败"); }
    }

    /**
     * 自动从剧本内容生成分镜（简化实现：按段落拆分）
     * 每约200字一个 segment，每个 segment 生成1个 shot
     */
    public void autoGenerate(Long projectId, Long scriptId) {
        Script script = scriptMapper.selectById(scriptId);
        if (script == null) {
            log.warn("[Storyboard] Script not found: {}", scriptId);
            return;
        }
        String content = script.getContent();
        if (content == null || content.isBlank()) {
            log.warn("[Storyboard] Script content is empty for scriptId={}", scriptId);
            return;
        }

        // 按段落拆分，每约200字合并为一个 segment
        String[] paragraphs = content.split("\n+");
        List<Segment> segments = new ArrayList<>();
        List<Shot> shots = new ArrayList<>();

        StringBuilder buf = new StringBuilder();
        int segIdx = 0;
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;
            buf.append(para).append("\n");
            if (buf.length() >= 200) {
                segIdx++;
                String segText = buf.toString().trim();
                buf.setLength(0);

                Segment seg = new Segment();
                seg.setIndex(segIdx);
                seg.setDescription(segText.length() > 50 ? segText.substring(0, 50) + "..." : segText);
                seg.setEmotion("neutral");
                seg.setAction("narrative");
                segments.add(seg);

                Shot shot = new Shot();
                shot.setId((long) segIdx);
                shot.setSegmentId(segIdx);
                shot.setTitle("分镜" + segIdx);
                shot.setFragmentContent(segText);
                shot.setCameraMotion("static");
                // 使用剧本文本作为 videoPrompt 占位
                shot.setVideoPrompt(segText.length() > 100 ? segText.substring(0, 100) : segText);
                shots.add(shot);
            }
        }
        // 处理剩余内容
        if (buf.length() > 0) {
            segIdx++;
            String segText = buf.toString().trim();
            Segment seg = new Segment();
            seg.setIndex(segIdx);
            seg.setDescription(segText.length() > 50 ? segText.substring(0, 50) + "..." : segText);
            seg.setEmotion("neutral");
            seg.setAction("narrative");
            segments.add(seg);

            Shot shot = new Shot();
            shot.setId((long) segIdx);
            shot.setSegmentId(segIdx);
            shot.setTitle("分镜" + segIdx);
            shot.setFragmentContent(segText);
            shot.setCameraMotion("static");
            shot.setVideoPrompt(segText.length() > 100 ? segText.substring(0, 100) : segText);
            shots.add(shot);
        }

        if (!segments.isEmpty()) {
            save(projectId, scriptId, segments, shots);
            log.info("[Storyboard] Auto-generated {} segments, {} shots for scriptId={}", segments.size(), shots.size(), scriptId);
        }
    }
}
