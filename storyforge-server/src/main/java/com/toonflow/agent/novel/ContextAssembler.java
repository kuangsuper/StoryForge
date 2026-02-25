package com.toonflow.agent.novel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.entity.*;
import com.toonflow.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 四层记忆上下文组装器。
 * 每章生成前自动组装精准上下文，确保 NovelWriter 在任何一章都能拿到足够且精准的上下文。
 *
 * Layer A: 固定记忆（世界观摘要+力量体系+主线+卷大纲+章概要）
 * Layer B: 角色记忆（按章概要 characters 召回角色档案+状态+对话风格）
 * Layer C: 短期记忆（前 N 章正文全文）
 * Layer D: 中长期记忆（本卷章摘要+前卷卷摘要+活跃伏笔）
 */
@Slf4j
@RequiredArgsConstructor
public class ContextAssembler {

    private final NovelWorldMapper novelWorldMapper;
    private final NovelCharacterMapper novelCharacterMapper;
    private final NovelOutlineMapper novelOutlineMapper;
    private final NovelChapterPlanMapper novelChapterPlanMapper;
    private final NovelMapper novelMapper;

    private static final int DEFAULT_SHORT_TERM_COUNT = 2;
    private static final int MAX_WORLD_SUMMARY_LENGTH = 2000;

    /**
     * 为指定章节组装完整的写作上下文
     */
    public String assemble(Long projectId, int chapterIndex) {
        NovelChapterPlan plan = novelChapterPlanMapper.selectOne(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getProjectId, projectId)
                        .eq(NovelChapterPlan::getChapterIndex, chapterIndex));

        String layerA = buildFixedMemory(projectId, plan);
        String layerB = buildCharacterMemory(projectId, plan);
        String layerC = buildShortTermMemory(projectId, chapterIndex, DEFAULT_SHORT_TERM_COUNT);
        String layerD = buildLongTermMemory(projectId, chapterIndex);

        return ToonSerializer.format(layerA, layerB, layerC, layerD);
    }

    /**
     * Layer A: 固定记忆 — 世界观摘要 + 力量体系 + 全书主线 + 当前卷大纲 + 当前章概要
     */
    private String buildFixedMemory(Long projectId, NovelChapterPlan plan) {
        StringBuilder sb = new StringBuilder();

        // 世界观摘要
        NovelWorld world = novelWorldMapper.selectOne(
                new LambdaQueryWrapper<NovelWorld>()
                        .eq(NovelWorld::getProjectId, projectId));
        if (world != null) {
            sb.append("[世界观]\n");
            if (world.getBackground() != null) {
                sb.append("背景: ").append(truncate(world.getBackground(), MAX_WORLD_SUMMARY_LENGTH)).append("\n");
            }
            if (world.getPowerSystem() != null) {
                sb.append("力量体系: ").append(truncate(world.getPowerSystem(), 500)).append("\n");
            }
            if (world.getCoreRules() != null) {
                sb.append("核心规则: ").append(truncate(world.getCoreRules(), 500)).append("\n");
            }
            if (world.getTaboos() != null) {
                sb.append("禁忌: ").append(truncate(world.getTaboos(), 300)).append("\n");
            }
            sb.append("\n");
        }

        // 全书主线（从第一条大纲记录获取）
        NovelOutline mainOutline = novelOutlineMapper.selectOne(
                new LambdaQueryWrapper<NovelOutline>()
                        .eq(NovelOutline::getProjectId, projectId)
                        .eq(NovelOutline::getVolumeIndex, 1)
                        .last("LIMIT 1"));
        if (mainOutline != null && mainOutline.getMainPlot() != null) {
            sb.append("[全书主线] ").append(truncate(mainOutline.getMainPlot(), 200)).append("\n\n");
        }

        // 当前卷大纲
        if (plan != null && plan.getVolumeIndex() != null) {
            NovelOutline volumeOutline = novelOutlineMapper.selectOne(
                    new LambdaQueryWrapper<NovelOutline>()
                            .eq(NovelOutline::getProjectId, projectId)
                            .eq(NovelOutline::getVolumeIndex, plan.getVolumeIndex()));
            if (volumeOutline != null) {
                sb.append("[当前卷] 第").append(volumeOutline.getVolumeIndex()).append("卷: ")
                  .append(safe(volumeOutline.getVolumeName())).append("\n");
                if (volumeOutline.getVolumePlot() != null) {
                    sb.append("卷主线: ").append(truncate(volumeOutline.getVolumePlot(), 1000)).append("\n");
                }
                if (volumeOutline.getVolumeClimax() != null) {
                    sb.append("卷高潮: ").append(volumeOutline.getVolumeClimax()).append("\n");
                }
                sb.append("\n");
            }
        }

        // 当前章概要
        if (plan != null) {
            sb.append("[当前章概要] 第").append(plan.getChapterIndex()).append("章: ")
              .append(safe(plan.getTitle())).append("\n");
            if (plan.getSummary() != null) sb.append("概要: ").append(plan.getSummary()).append("\n");
            if (plan.getKeyEvents() != null) sb.append("核心事件: ").append(plan.getKeyEvents()).append("\n");
            if (plan.getEmotionCurve() != null) sb.append("情绪曲线: ").append(plan.getEmotionCurve()).append("\n");
            if (plan.getForeshadowing() != null) sb.append("本章伏笔: ").append(plan.getForeshadowing()).append("\n");
            if (plan.getPayoff() != null) sb.append("回收伏笔: ").append(plan.getPayoff()).append("\n");
            if (plan.getCliffhanger() != null) sb.append("章末悬念: ").append(plan.getCliffhanger()).append("\n");
            if (plan.getWordTarget() != null) sb.append("目标字数: ").append(plan.getWordTarget()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Layer B: 角色记忆 — 根据章概要 characters 字段召回对应角色档案
     */
    private String buildCharacterMemory(Long projectId, NovelChapterPlan plan) {
        if (plan == null || plan.getCharacters() == null || plan.getCharacters().isEmpty()) {
            return "";
        }

        // 解析章概要中的角色名列表
        List<String> characterNames = parseCharacterNames(plan.getCharacters());
        if (characterNames.isEmpty()) return "";

        // 查询所有项目角色
        List<NovelCharacter> allCharacters = novelCharacterMapper.selectList(
                new LambdaQueryWrapper<NovelCharacter>()
                        .eq(NovelCharacter::getProjectId, projectId));

        // 只召回本章出场角色
        List<NovelCharacter> relevantCharacters = allCharacters.stream()
                .filter(c -> characterNames.contains(c.getName()))
                .collect(Collectors.toList());

        return ToonSerializer.toCharacterDetail(relevantCharacters);
    }

    /**
     * Layer C: 短期记忆 — 前 N 章正文全文
     */
    private String buildShortTermMemory(Long projectId, int chapterIndex, int count) {
        if (chapterIndex <= 1) return "";

        int fromIndex = Math.max(1, chapterIndex - count);
        List<Novel> previousChapters = novelMapper.selectList(
                new LambdaQueryWrapper<Novel>()
                        .eq(Novel::getProjectId, projectId)
                        .ge(Novel::getChapterIndex, fromIndex)
                        .lt(Novel::getChapterIndex, chapterIndex)
                        .orderByAsc(Novel::getChapterIndex));

        if (previousChapters.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (Novel novel : previousChapters) {
            sb.append("--- 第").append(novel.getChapterIndex()).append("章: ")
              .append(safe(novel.getChapter())).append(" ---\n");
            if (novel.getChapterData() != null) {
                sb.append(novel.getChapterData()).append("\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * Layer D: 中长期记忆 — 本卷已写章摘要 + 前卷卷摘要 + 活跃伏笔清单
     */
    private String buildLongTermMemory(Long projectId, int chapterIndex) {
        StringBuilder sb = new StringBuilder();

        // 获取当前章所属卷
        NovelChapterPlan currentPlan = novelChapterPlanMapper.selectOne(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getProjectId, projectId)
                        .eq(NovelChapterPlan::getChapterIndex, chapterIndex));
        int currentVolume = currentPlan != null && currentPlan.getVolumeIndex() != null
                ? currentPlan.getVolumeIndex() : 1;

        // 本卷已写章摘要
        List<Novel> volumeChapters = novelMapper.selectList(
                new LambdaQueryWrapper<Novel>()
                        .eq(Novel::getProjectId, projectId)
                        .eq(Novel::getVolumeIndex, currentVolume)
                        .lt(Novel::getChapterIndex, chapterIndex)
                        .select(Novel::getChapterIndex, Novel::getChapter, Novel::getSummary)
                        .orderByAsc(Novel::getChapterIndex));
        if (!volumeChapters.isEmpty()) {
            sb.append("[本卷已写章摘要]\n");
            List<Map<String, Object>> summaries = new ArrayList<>();
            for (Novel n : volumeChapters) {
                summaries.add(Map.of(
                        "chapterIndex", n.getChapterIndex(),
                        "title", safe(n.getChapter()),
                        "summary", safe(n.getSummary())));
            }
            sb.append(ToonSerializer.toChapterSummaryList(summaries)).append("\n");
        }

        // 前卷卷摘要
        if (currentVolume > 1) {
            List<NovelOutline> previousVolumes = novelOutlineMapper.selectList(
                    new LambdaQueryWrapper<NovelOutline>()
                            .eq(NovelOutline::getProjectId, projectId)
                            .lt(NovelOutline::getVolumeIndex, currentVolume)
                            .orderByAsc(NovelOutline::getVolumeIndex));
            if (!previousVolumes.isEmpty()) {
                sb.append("[前卷摘要]\n");
                for (NovelOutline vol : previousVolumes) {
                    sb.append("第").append(vol.getVolumeIndex()).append("卷 ")
                      .append(safe(vol.getVolumeName())).append(": ")
                      .append(truncate(safe(vol.getVolumePlot()), 500)).append("\n");
                }
                sb.append("\n");
            }
        }

        // 活跃伏笔清单
        List<Map<String, Object>> activeForeshadowing = getActiveForeshadowing(projectId, chapterIndex);
        if (!activeForeshadowing.isEmpty()) {
            sb.append("[活跃伏笔（未回收）]\n");
            sb.append(ToonSerializer.toForeshadowingTable(activeForeshadowing));
        }

        return sb.toString();
    }

    /**
     * 获取未回收的伏笔清单
     */
    List<Map<String, Object>> getActiveForeshadowing(Long projectId, int beforeChapter) {
        List<NovelChapterPlan> plans = novelChapterPlanMapper.selectList(
                new LambdaQueryWrapper<NovelChapterPlan>()
                        .eq(NovelChapterPlan::getProjectId, projectId)
                        .lt(NovelChapterPlan::getChapterIndex, beforeChapter)
                        .orderByAsc(NovelChapterPlan::getChapterIndex));

        // 收集所有已埋设的伏笔
        List<Map<String, Object>> planted = new ArrayList<>();
        Set<String> resolved = new HashSet<>();

        for (NovelChapterPlan plan : plans) {
            // 解析 foreshadowing
            if (plan.getForeshadowing() != null && !plan.getForeshadowing().isEmpty()) {
                List<String> items = parseJsonArray(plan.getForeshadowing());
                for (String item : items) {
                    Map<String, Object> fs = new HashMap<>();
                    fs.put("plantedChapter", plan.getChapterIndex());
                    fs.put("content", item);
                    planted.add(fs);
                }
            }
            // 解析 payoff（已回收的伏笔）
            if (plan.getPayoff() != null && !plan.getPayoff().isEmpty()) {
                List<String> items = parseJsonArray(plan.getPayoff());
                resolved.addAll(items);
            }
        }

        // 过滤掉已回收的
        return planted.stream()
                .filter(fs -> !resolved.contains(String.valueOf(fs.get("content"))))
                .collect(Collectors.toList());
    }

    private List<String> parseCharacterNames(String charactersField) {
        if (charactersField == null || charactersField.isEmpty()) return List.of();
        try {
            // 尝试 JSON 数组解析
            if (charactersField.trim().startsWith("[")) {
                ObjectMapper om = new ObjectMapper();
                return om.readValue(charactersField, new TypeReference<>() {});
            }
        } catch (Exception ignored) {}
        // 降级：逗号分隔
        return Arrays.stream(charactersField.split("[,，、]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) return List.of();
        try {
            if (json.trim().startsWith("[")) {
                ObjectMapper om = new ObjectMapper();
                return om.readValue(json, new TypeReference<>() {});
            }
        } catch (Exception ignored) {}
        // 降级：逗号分隔
        return Arrays.stream(json.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}
