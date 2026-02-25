package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.ai.model.AiRequest;
import com.toonflow.ai.model.ChatMessage;
import com.toonflow.ai.provider.TextAiProvider;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Novel;
import com.toonflow.entity.Outline;
import com.toonflow.entity.Script;
import com.toonflow.entity.json.AssetItem;
import com.toonflow.entity.json.EpisodeData;
import com.toonflow.mapper.NovelMapper;
import com.toonflow.mapper.OutlineMapper;
import com.toonflow.mapper.ScriptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScriptService {

    private final ScriptMapper scriptMapper;
    private final OutlineMapper outlineMapper;
    private final NovelMapper novelMapper;
    private final AiProviderService aiProviderService;
    private final PromptService promptService;

    public Script getById(Long id) {
        Script script = scriptMapper.selectById(id);
        if (script == null) throw new BizException(ErrorCode.NOT_FOUND);
        return script;
    }

    public List<Script> listByProject(Long projectId) {
        return scriptMapper.selectList(
                new LambdaQueryWrapper<Script>().eq(Script::getProjectId, projectId));
    }

    public void update(Long id, String content) {
        Script script = scriptMapper.selectById(id);
        if (script == null) throw new BizException(ErrorCode.NOT_FOUND);
        script.setContent(content);
        scriptMapper.updateById(script);
    }

    /**
     * AI 生成剧本：基于大纲 + 原文章节
     */
    public String generate(Long scriptId, Long outlineId) {
        Script script = scriptMapper.selectById(scriptId);
        if (script == null) throw new BizException(ErrorCode.NOT_FOUND);

        Outline outline = outlineMapper.selectById(outlineId);
        if (outline == null) throw new BizException(ErrorCode.NOT_FOUND, "大纲不存在");

        EpisodeData data = outline.getData();
        if (data == null) throw new BizException(ErrorCode.BAD_REQUEST, "大纲数据为空");

        // 获取章节原文
        StringBuilder chapterText = new StringBuilder();
        if (data.getChapterRange() != null && !data.getChapterRange().isEmpty()) {
            for (Integer idx : data.getChapterRange()) {
                Novel novel = novelMapper.selectOne(
                        new LambdaQueryWrapper<Novel>()
                                .eq(Novel::getProjectId, script.getProjectId())
                                .eq(Novel::getChapterIndex, idx));
                if (novel != null && novel.getChapterData() != null) {
                    chapterText.append("第").append(idx).append("章:\n")
                            .append(novel.getChapterData()).append("\n\n");
                }
            }
        }

        // 组装 user prompt
        String userPrompt = buildScriptPrompt(data, chapterText.toString());

        String systemPrompt = promptService.getPromptValue("script");
        TextAiProvider provider = aiProviderService.getTextProvider("generateScript");
        AiRequest request = AiRequest.builder()
                .systemPrompt(systemPrompt)
                .messages(List.of(ChatMessage.user(userPrompt)))
                .build();

        String result = provider.invoke(request);
        script.setContent(result);
        scriptMapper.updateById(script);
        return result;
    }

    private String buildScriptPrompt(EpisodeData data, String chapterText) {
        StringBuilder sb = new StringBuilder();
        sb.append("场景: ").append(formatAssetItems(data.getScenes())).append("\n");
        sb.append("角色: ").append(formatAssetItems(data.getCharacters())).append("\n");
        sb.append("道具: ").append(formatAssetItems(data.getProps())).append("\n");
        sb.append("核心矛盾: ").append(nullSafe(data.getCoreConflict())).append("\n");
        sb.append("剧情主干: ").append(nullSafe(data.getOutline())).append("\n");
        sb.append("开场镜头: ").append(nullSafe(data.getOpeningHook())).append("\n");

        if (data.getKeyEvents() != null && data.getKeyEvents().size() >= 4) {
            sb.append("剧情节点: 起:").append(data.getKeyEvents().get(0))
              .append(" 承:").append(data.getKeyEvents().get(1))
              .append(" 转:").append(data.getKeyEvents().get(2))
              .append(" 合:").append(data.getKeyEvents().get(3)).append("\n");
        }

        sb.append("情绪曲线: ").append(nullSafe(data.getEmotionalCurve())).append("\n");
        if (data.getVisualHighlights() != null) {
            sb.append("视觉重点: ").append(String.join(", ", data.getVisualHighlights())).append("\n");
        }
        sb.append("结尾悬念: ").append(nullSafe(data.getEndingHook())).append("\n");
        if (data.getClassicQuotes() != null) {
            sb.append("金句: ").append(String.join(", ", data.getClassicQuotes())).append("\n");
        }

        if (!chapterText.isEmpty()) {
            sb.append("\n原文参考:\n").append(chapterText);
        }
        return sb.toString();
    }

    private String formatAssetItems(List<AssetItem> items) {
        if (items == null || items.isEmpty()) return "无";
        return items.stream()
                .map(i -> i.getName() + "(" + nullSafe(i.getDescription()) + ")")
                .collect(Collectors.joining(", "));
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
