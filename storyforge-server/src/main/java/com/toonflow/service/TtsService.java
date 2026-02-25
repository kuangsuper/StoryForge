package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.ai.model.TtsRequest;
import com.toonflow.ai.provider.TtsAiProvider;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.entity.Assets;
import com.toonflow.entity.Script;
import com.toonflow.entity.TtsAudio;
import com.toonflow.entity.TtsConfig;
import com.toonflow.mapper.AssetsMapper;
import com.toonflow.mapper.ScriptMapper;
import com.toonflow.mapper.TtsAudioMapper;
import com.toonflow.mapper.TtsConfigMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    private final TtsAudioMapper ttsAudioMapper;
    private final TtsConfigMapper ttsConfigMapper;
    private final ScriptMapper scriptMapper;
    private final AssetsMapper assetsMapper;
    private final AiProviderService aiProviderService;
    private final OssService ossService;

    private static final Pattern DLG = Pattern.compile("^(.+?)[:：](.+)$");

    @Data
    public static class DialogueLine {
        private String characterName;
        private String text;
        private boolean narration;
    }

    /**
     * 从剧本内容提取对话行
     */
    public List<DialogueLine> extractDialogues(String scriptContent) {
        if (scriptContent == null || scriptContent.isBlank()) return Collections.emptyList();
        List<DialogueLine> lines = new ArrayList<>();
        for (String raw : scriptContent.split("\n")) {
            raw = raw.trim();
            if (raw.isEmpty()) continue;

            DialogueLine dl = new DialogueLine();
            // 匹配 【旁白】文本
            if (raw.startsWith("【") && raw.contains("】")) {
                int end = raw.indexOf('】');
                String tag = raw.substring(1, end).trim();
                dl.setCharacterName(tag);
                dl.setText(raw.substring(end + 1).trim());
                dl.setNarration("旁白".equals(tag));
                if (!dl.getText().isEmpty()) lines.add(dl);
                continue;
            }
            // 匹配 角色：台词 / 角色:台词
            Matcher m = DLG.matcher(raw);
            if (m.matches()) {
                dl.setCharacterName(m.group(1).trim());
                dl.setText(m.group(2).trim());
                dl.setNarration(false);
                if (!dl.getText().isEmpty()) lines.add(dl);
            }
        }
        return lines;
    }

    /**
     * 为对话行匹配语音配置：精确匹配 → 模糊匹配 → 默认配置
     */
    public Map<String, TtsConfig> matchVoices(Long projectId, List<DialogueLine> dialogues) {
        // 查询项目角色资产
        List<Assets> roles = assetsMapper.selectList(
                new LambdaQueryWrapper<Assets>()
                        .eq(Assets::getProjectId, projectId)
                        .eq(Assets::getType, "role"));

        // 查询项目 TTS 配置
        List<TtsConfig> configs = ttsConfigMapper.selectList(
                new LambdaQueryWrapper<TtsConfig>()
                        .eq(TtsConfig::getProjectId, projectId));

        // 按 characterId 索引配置
        Map<Long, TtsConfig> configByCharId = new HashMap<>();
        TtsConfig defaultConfig = null;
        for (TtsConfig c : configs) {
            if (c.getCharacterId() == null) {
                defaultConfig = c;
            } else {
                configByCharId.put(c.getCharacterId(), c);
            }
        }

        // 按角色名索引资产
        Map<String, Assets> roleByName = new HashMap<>();
        for (Assets r : roles) {
            if (r.getName() != null) roleByName.put(r.getName(), r);
        }

        Map<String, TtsConfig> result = new HashMap<>();
        for (DialogueLine dl : dialogues) {
            String name = dl.getCharacterName();
            if (result.containsKey(name)) continue;

            // 旁白 → 默认配置
            if (dl.isNarration()) {
                result.put(name, defaultConfig);
                continue;
            }

            // 精确匹配
            Assets role = roleByName.get(name);
            if (role != null && configByCharId.containsKey(role.getId())) {
                result.put(name, configByCharId.get(role.getId()));
                continue;
            }

            // 模糊匹配（名称包含）
            TtsConfig fuzzy = null;
            for (Map.Entry<String, Assets> entry : roleByName.entrySet()) {
                if (entry.getKey().contains(name) || name.contains(entry.getKey())) {
                    Assets matched = entry.getValue();
                    if (configByCharId.containsKey(matched.getId())) {
                        fuzzy = configByCharId.get(matched.getId());
                        break;
                    }
                }
            }
            result.put(name, fuzzy != null ? fuzzy : defaultConfig);
        }
        return result;
    }

    /**
     * 完整 TTS 生成流程：提取对话 → 匹配语音 → 合成 → OSS 上传 → 保存记录
     */
    public void generate(Long projectId, Long scriptId) {
        Script script = scriptMapper.selectById(scriptId);
        if (script == null || script.getContent() == null) {
            log.warn("[TTS] Script not found or empty: {}", scriptId);
            return;
        }

        List<DialogueLine> dialogues = extractDialogues(script.getContent());
        if (dialogues.isEmpty()) {
            log.info("[TTS] No dialogues extracted from scriptId={}", scriptId);
            return;
        }

        Map<String, TtsConfig> voiceMap = matchVoices(projectId, dialogues);
        TtsAiProvider provider = aiProviderService.getTtsProvider("tts");

        for (DialogueLine dl : dialogues) {
            try {
                TtsConfig config = voiceMap.get(dl.getCharacterName());
                String voiceId = config != null && config.getVoiceId() != null ? config.getVoiceId() : "default";
                double speed = config != null && config.getSpeed() != null ? config.getSpeed().doubleValue() : 1.0;
                double pitch = config != null && config.getPitch() != null ? config.getPitch().doubleValue() : 1.0;
                String emotion = config != null && config.getEmotion() != null ? config.getEmotion() : "neutral";

                TtsRequest req = TtsRequest.builder()
                        .text(dl.getText())
                        .voiceId(voiceId)
                        .speed(speed)
                        .pitch(pitch)
                        .emotion(emotion)
                        .outputFormat("mp3")
                        .build();

                byte[] audio = provider.synthesize(req);
                if (audio == null || audio.length == 0) {
                    log.warn("[TTS] Empty audio for character={}, text={}", dl.getCharacterName(), dl.getText());
                    continue;
                }

                // 上传到 OSS
                String key = "tts/" + projectId + "/" + UUID.randomUUID() + ".mp3";
                String url = ossService.upload(key, audio, "audio/mpeg");

                // 保存记录
                TtsAudio record = new TtsAudio();
                record.setProjectId(projectId);
                record.setScriptId(scriptId);
                record.setText(dl.getText());
                record.setCharacterName(dl.getCharacterName());
                record.setVoiceId(voiceId);
                record.setFilePath(url);
                record.setState(1);
                ttsAudioMapper.insert(record);

            } catch (Exception e) {
                log.error("[TTS] Failed for character={}: {}", dl.getCharacterName(), e.getMessage());
            }
        }
        log.info("[TTS] Generated {} audio records for scriptId={}", dialogues.size(), scriptId);
    }

    /**
     * 查询已生成的 TTS 音频列表
     */
    public List<TtsAudio> listAudio(Long projectId, Long scriptId) {
        return ttsAudioMapper.selectList(
                new LambdaQueryWrapper<TtsAudio>()
                        .eq(TtsAudio::getProjectId, projectId)
                        .eq(TtsAudio::getScriptId, scriptId)
                        .orderByAsc(TtsAudio::getId));
    }

    /**
     * 获取可用语音列表
     */
    public List<Map<String, String>> getVoices(String manufacturer) {
        List<Map<String, String>> voices = new ArrayList<>();
        voices.add(Map.of("id", "zh-CN-XiaoxiaoNeural", "name", "晓晓（女）", "manufacturer", "azure"));
        voices.add(Map.of("id", "zh-CN-YunxiNeural", "name", "云希（男）", "manufacturer", "azure"));
        voices.add(Map.of("id", "zh-CN-YunjianNeural", "name", "云健（男）", "manufacturer", "azure"));
        voices.add(Map.of("id", "zh-CN-XiaoyiNeural", "name", "晓伊（女）", "manufacturer", "azure"));
        voices.add(Map.of("id", "alloy", "name", "Alloy", "manufacturer", "openai"));
        voices.add(Map.of("id", "echo", "name", "Echo", "manufacturer", "openai"));
        voices.add(Map.of("id", "fable", "name", "Fable", "manufacturer", "openai"));
        voices.add(Map.of("id", "onyx", "name", "Onyx", "manufacturer", "openai"));
        voices.add(Map.of("id", "nova", "name", "Nova", "manufacturer", "openai"));
        voices.add(Map.of("id", "shimmer", "name", "Shimmer", "manufacturer", "openai"));

        if (manufacturer != null && !manufacturer.isBlank()) {
            voices.removeIf(v -> !manufacturer.equalsIgnoreCase(v.get("manufacturer")));
        }
        return voices;
    }

    /**
     * 单条 TTS 预览（不保存）
     */
    public byte[] preview(String text, String voiceId, String manufacturer) {
        TtsAiProvider provider = aiProviderService.getTtsProvider("tts");
        TtsRequest req = TtsRequest.builder()
                .text(text)
                .voiceId(voiceId != null ? voiceId : "default")
                .speed(1.0)
                .pitch(1.0)
                .outputFormat("mp3")
                .build();
        return provider.synthesize(req);
    }
}
