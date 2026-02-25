package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.ai.model.AiRequest;
import com.toonflow.ai.model.ChatMessage;
import com.toonflow.ai.provider.TextAiProvider;
import com.toonflow.ai.service.AiProviderService;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Assets;
import com.toonflow.entity.Outline;
import com.toonflow.entity.json.AssetItem;
import com.toonflow.entity.json.EpisodeData;
import com.toonflow.mapper.AssetsMapper;
import com.toonflow.mapper.OutlineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AssetsService {

    private final AssetsMapper assetsMapper;
    private final OutlineMapper outlineMapper;
    private final AiProviderService aiProviderService;
    private final PromptService promptService;

    public List<Assets> list(Long projectId, String type) {
        LambdaQueryWrapper<Assets> query = new LambdaQueryWrapper<Assets>()
                .eq(Assets::getProjectId, projectId);
        if (type != null && !type.isBlank()) {
            query.eq(Assets::getType, type);
        }
        return assetsMapper.selectList(query);
    }

    public Assets create(Long projectId, Assets asset) {
        asset.setProjectId(projectId);
        assetsMapper.insert(asset);
        return asset;
    }

    public void update(Long id, Assets asset) {
        Assets existing = assetsMapper.selectById(id);
        if (existing == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (asset.getName() != null) existing.setName(asset.getName());
        if (asset.getIntro() != null) existing.setIntro(asset.getIntro());
        if (asset.getPrompt() != null) existing.setPrompt(asset.getPrompt());
        assetsMapper.updateById(existing);
    }

    public void delete(Long id) {
        Assets existing = assetsMapper.selectById(id);
        if (existing == null) throw new BizException(ErrorCode.NOT_FOUND);
        assetsMapper.deleteById(id);
    }

    @Transactional
    public void batchSave(Long projectId, List<Assets> assets) {
        if (assets == null || assets.isEmpty()) return;
        for (Assets asset : assets) {
            asset.setProjectId(projectId);
            if (asset.getId() != null) {
                assetsMapper.updateById(asset);
            } else {
                assetsMapper.insert(asset);
            }
        }
    }

    /**
     * 从大纲中提取资产，去重逻辑：同 name+type 保留最长 description
     */
    @Transactional
    public void extractFromOutlines(Long projectId) {
        List<Outline> outlines = outlineMapper.selectList(
                new LambdaQueryWrapper<Outline>().eq(Outline::getProjectId, projectId));
        Map<String, Assets> assetMap = new LinkedHashMap<>();
        for (Outline outline : outlines) {
            EpisodeData data = outline.getData();
            if (data == null) continue;
            collectAssets(assetMap, projectId, data.getCharacters(), "role");
            collectAssets(assetMap, projectId, data.getProps(), "props");
            collectAssets(assetMap, projectId, data.getScenes(), "scene");
        }
        for (Assets asset : assetMap.values()) {
            Assets existing = assetsMapper.selectOne(
                    new LambdaQueryWrapper<Assets>()
                            .eq(Assets::getProjectId, projectId)
                            .eq(Assets::getName, asset.getName())
                            .eq(Assets::getType, asset.getType()));
            if (existing == null) {
                assetsMapper.insert(asset);
            } else {
                existing.setIntro(asset.getIntro());
                assetsMapper.updateById(existing);
            }
        }
    }

    private void collectAssets(Map<String, Assets> map, Long projectId,
                               List<AssetItem> items, String type) {
        if (items == null) return;
        for (AssetItem item : items) {
            String key = type + ":" + item.getName();
            Assets existing = map.get(key);
            String desc = item.getDescription() != null ? item.getDescription() : "";
            if (existing == null) {
                Assets a = new Assets();
                a.setProjectId(projectId);
                a.setName(item.getName());
                a.setIntro(desc);
                a.setType(type);
                map.put(key, a);
            } else {
                // 保留最长 description
                if (desc.length() > (existing.getIntro() != null ? existing.getIntro().length() : 0)) {
                    existing.setIntro(desc);
                }
            }
        }
    }

    /**
     * 润色资产提示词
     */
    public String polishPrompt(Long id) {
        Assets asset = assetsMapper.selectById(id);
        if (asset == null) throw new BizException(ErrorCode.NOT_FOUND);

        TextAiProvider provider = aiProviderService.getTextProvider("generateScript");
        String systemPrompt = promptService.getPromptValue("polish-prompt");
        String userPrompt = "资产类型: " + asset.getType() + "\n名称: " + asset.getName()
                + "\n描述: " + (asset.getIntro() != null ? asset.getIntro() : "")
                + "\n当前提示词: " + (asset.getPrompt() != null ? asset.getPrompt() : "");

        AiRequest request = AiRequest.builder()
                .systemPrompt(systemPrompt)
                .messages(List.of(ChatMessage.user(userPrompt)))
                .build();
        String result = provider.invoke(request);
        asset.setPrompt(result);
        assetsMapper.updateById(asset);
        return result;
    }
}
