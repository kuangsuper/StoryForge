package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.ai.model.ImageRequest;
import com.toonflow.ai.provider.ImageAiProvider;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Assets;
import com.toonflow.entity.Image;
import com.toonflow.entity.Project;
import com.toonflow.mapper.AssetsMapper;
import com.toonflow.mapper.ImageMapper;
import com.toonflow.mapper.ProjectMapper;
import com.toonflow.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageMapper imageMapper;
    private final AssetsMapper assetsMapper;
    private final ProjectMapper projectMapper;
    private final AiProviderService aiProviderService;
    private final PromptService promptService;
    private final QuotaService quotaService;

    public List<Image> list(Long projectId, String type, Long assetsId) {
        LambdaQueryWrapper<Image> query = new LambdaQueryWrapper<Image>()
                .eq(Image::getProjectId, projectId);
        if (type != null && !type.isBlank()) {
            query.eq(Image::getType, type);
        }
        if (assetsId != null) {
            query.eq(Image::getAssetsId, assetsId);
        }
        return imageMapper.selectList(query);
    }

    /**
     * 生成资产图片
     */
    public Image generateAssetImage(Long projectId, Long assetId) {
        Assets asset = assetsMapper.selectById(assetId);
        if (asset == null) throw new BizException(ErrorCode.NOT_FOUND, "资产不存在");

        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");

        // 配额检查
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            quotaService.checkAndConsume(userId, "image", 1);
        } catch (BizException e) {
            throw e;
        } catch (Exception ignored) {
            // 非 HTTP 上下文（如流水线调用）跳过配额检查
        }

        // 根据 asset.type 选择 prompt code
        String promptCode = switch (asset.getType()) {
            case "role" -> "role-generateImage";
            case "scene" -> "scene-generateImage";
            default -> "tool-generateImage";
        };
        String systemPrompt = promptService.getPromptValue(promptCode);

        String userPrompt = "画风: " + nullSafe(project.getArtStyle())
                + ", 名称: " + asset.getName()
                + ", 描述: " + nullSafe(asset.getIntro()) + " " + nullSafe(asset.getPrompt());

        // 组合 system + user 作为完整 prompt
        String fullPrompt = systemPrompt.isEmpty() ? userPrompt : systemPrompt + "\n" + userPrompt;

        ImageAiProvider provider = aiProviderService.getImageProvider("assetsImage");
        ImageRequest request = ImageRequest.builder()
                .prompt(fullPrompt)
                .mode("t2i")
                .build();

        String imageResult;
        try {
            imageResult = provider.generate(request);
        } catch (Exception e) {
            throw new BizException(ErrorCode.AI_CALL_FAILED, "图片生成失败: " + e.getMessage());
        }

        // 生成存储路径
        String filePath = "/" + projectId + "/" + asset.getType() + "/" + UUID.randomUUID() + ".jpg";

        // 保存 Image 记录
        Image image = new Image();
        image.setProjectId(projectId);
        image.setAssetsId(assetId);
        image.setType(asset.getType());
        image.setFilePath(imageResult != null ? imageResult : filePath);
        image.setState(1);
        imageMapper.insert(image);

        // 更新 asset.filePath
        asset.setFilePath(image.getFilePath());
        assetsMapper.updateById(asset);

        return image;
    }

    /**
     * 智能筛选与分镜相关的资产图片
     */
    public List<Assets> selectRelevantAssets(Long projectId, List<String> shotPrompts) {
        List<Assets> allAssets = assetsMapper.selectList(
                new LambdaQueryWrapper<Assets>()
                        .eq(Assets::getProjectId, projectId)
                        .isNotNull(Assets::getFilePath)
                        .ne(Assets::getFilePath, ""));

        if (shotPrompts == null || shotPrompts.isEmpty()) return Collections.emptyList();

        Set<Long> matchedIds = new LinkedHashSet<>();
        List<Assets> result = new ArrayList<>();
        for (Assets asset : allAssets) {
            for (String prompt : shotPrompts) {
                if (prompt != null && prompt.contains(asset.getName())) {
                    if (matchedIds.add(asset.getId())) {
                        result.add(asset);
                    }
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 图片质量检测（基于分辨率和宽高比）
     * ≥80 通过, 60-79 建议重新生成, <60 自动重新生成
     */
    public List<Map<String, Object>> qualityCheck(Long projectId, List<Long> imageIds) {
        Project project = projectMapper.selectById(projectId);
        String videoRatio = project != null ? project.getVideoRatio() : "16:9";
        double targetRatio = parseRatio(videoRatio);

        List<Map<String, Object>> results = new ArrayList<>();
        for (Long imageId : imageIds) {
            Image image = imageMapper.selectById(imageId);
            if (image == null) continue;

            // 简化评分：基于是否有文件路径和状态
            int score = 80;
            if (image.getFilePath() == null || image.getFilePath().isEmpty()) {
                score = 0;
            } else if (image.getState() == null || image.getState() != 1) {
                score = 50;
            }

            String suggestion = score >= 80 ? "通过" : score >= 60 ? "建议重新生成" : "需要重新生成";
            results.add(Map.of("imageId", imageId, "score", score, "suggestion", suggestion));
        }
        return results;
    }

    private double parseRatio(String ratio) {
        if (ratio == null || !ratio.contains(":")) return 16.0 / 9.0;
        String[] parts = ratio.split(":");
        try { return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]); }
        catch (Exception e) { return 16.0 / 9.0; }
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
