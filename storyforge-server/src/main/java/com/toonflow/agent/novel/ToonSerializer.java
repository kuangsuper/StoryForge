package com.toonflow.agent.novel;

import com.toonflow.entity.NovelCharacter;
import com.toonflow.entity.NovelChapterPlan;

import java.util.List;
import java.util.Map;

/**
 * TOON (Token-Oriented Object Notation) 格式序列化器。
 * 将结构化数据转为省 token 的紧凑格式，节省 30-60% token 消耗。
 * 正文内容保持纯文本，不转 TOON。
 */
public class ToonSerializer {

    /**
     * 组装四层记忆为最终 Prompt 字符串
     */
    public static String format(String layerA, String layerB, String layerC, String layerD) {
        StringBuilder sb = new StringBuilder();
        if (layerA != null && !layerA.isEmpty()) {
            sb.append("=== 固定记忆 ===\n").append(layerA).append("\n\n");
        }
        if (layerB != null && !layerB.isEmpty()) {
            sb.append("=== 角色记忆 ===\n").append(layerB).append("\n\n");
        }
        if (layerC != null && !layerC.isEmpty()) {
            sb.append("=== 短期记忆（前文正文）===\n").append(layerC).append("\n\n");
        }
        if (layerD != null && !layerD.isEmpty()) {
            sb.append("=== 中长期记忆 ===\n").append(layerD);
        }
        return sb.toString();
    }

    /**
     * 角色列表 → TOON 表格格式
     * 输出示例:
     * characters[2]{name,role,powerLevel,location,physicalState,emotionalState,speechTone}:
     *   陈默,protagonist,凝源者·中期,猎人公会总部,左臂轻伤,警惕压抑,冷淡简短
     *   林晚,supporting,凝源者·初期,医疗区,健康,担忧焦虑,温柔关切
     */
    public static String toCharacterTable(List<NovelCharacter> characters) {
        if (characters == null || characters.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("characters[").append(characters.size())
          .append("]{name,role,age,appearance,personality,ability,speechTone}:\n");
        for (NovelCharacter c : characters) {
            sb.append("  ")
              .append(safe(c.getName())).append(",")
              .append(safe(c.getRole())).append(",")
              .append(c.getAge() != null ? c.getAge() : "-").append(",")
              .append(truncField(c.getAppearance(), 80)).append(",")
              .append(truncField(c.getPersonality(), 80)).append(",")
              .append(truncField(c.getAbility(), 80)).append(",")
              .append(extractTone(c.getSpeechStyle()))
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * 角色完整档案（含 currentState 和 speechStyle），用于 Layer B
     */
    public static String toCharacterDetail(List<NovelCharacter> characters) {
        if (characters == null || characters.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (NovelCharacter c : characters) {
            sb.append("[角色] ").append(safe(c.getName()))
              .append(" (").append(safe(c.getRole())).append(")\n");
            if (c.getAppearance() != null) sb.append("外貌: ").append(c.getAppearance()).append("\n");
            if (c.getPersonality() != null) sb.append("性格: ").append(c.getPersonality()).append("\n");
            if (c.getAbility() != null) sb.append("能力: ").append(c.getAbility()).append("\n");
            if (c.getRelationships() != null) sb.append("关系: ").append(c.getRelationships()).append("\n");
            if (c.getCurrentState() != null) sb.append("当前状态: ").append(c.getCurrentState()).append("\n");
            if (c.getSpeechStyle() != null) sb.append("说话风格: ").append(c.getSpeechStyle()).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 章概要列表 → TOON 表格格式
     */
    public static String toChapterPlanTable(List<NovelChapterPlan> plans) {
        if (plans == null || plans.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("chapterPlans[").append(plans.size())
          .append("]{chapterIndex,volumeIndex,title,emotionCurve,cliffhanger}:\n");
        for (NovelChapterPlan p : plans) {
            sb.append("  ")
              .append(p.getChapterIndex() != null ? p.getChapterIndex() : "-").append(",")
              .append(p.getVolumeIndex() != null ? p.getVolumeIndex() : "-").append(",")
              .append(safe(p.getTitle())).append(",")
              .append(safe(p.getEmotionCurve())).append(",")
              .append(truncField(p.getCliffhanger(), 60))
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * 章摘要列表 → 紧凑格式（用于 Layer D 中长期记忆）
     */
    public static String toChapterSummaryList(List<Map<String, Object>> summaries) {
        if (summaries == null || summaries.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("chapterSummaries[").append(summaries.size()).append("]:\n");
        for (Map<String, Object> s : summaries) {
            sb.append("  第").append(s.getOrDefault("chapterIndex", "?")).append("章 ")
              .append(s.getOrDefault("title", "")).append(": ")
              .append(truncField(String.valueOf(s.getOrDefault("summary", "")), 200))
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * 伏笔清单 → TOON 表格格式
     */
    public static String toForeshadowingTable(List<Map<String, Object>> foreshadowings) {
        if (foreshadowings == null || foreshadowings.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("activeForeshadowing[").append(foreshadowings.size())
          .append("]{plantedChapter,content}:\n");
        for (Map<String, Object> f : foreshadowings) {
            sb.append("  ")
              .append(f.getOrDefault("plantedChapter", "?")).append(",")
              .append(truncField(String.valueOf(f.getOrDefault("content", "")), 100))
              .append("\n");
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return s != null ? s.replace(",", "，").replace("\n", " ") : "-";
    }

    private static String truncField(String s, int maxLen) {
        if (s == null) return "-";
        s = s.replace(",", "，").replace("\n", " ");
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private static String extractTone(String speechStyleJson) {
        if (speechStyleJson == null || speechStyleJson.isEmpty()) return "-";
        // 简单提取 tone 字段值，避免引入 JSON 解析依赖
        int idx = speechStyleJson.indexOf("\"tone\"");
        if (idx < 0) return "-";
        int start = speechStyleJson.indexOf("\"", idx + 6);
        if (start < 0) return "-";
        int end = speechStyleJson.indexOf("\"", start + 1);
        if (end < 0) return "-";
        return speechStyleJson.substring(start + 1, end).replace(",", "，");
    }
}
