package com.toonflow.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.ai.factory.ImageAiProviderFactory;
import com.toonflow.ai.factory.TextAiProviderFactory;
import com.toonflow.ai.factory.TtsAiProviderFactory;
import com.toonflow.ai.factory.VideoAiProviderFactory;
import com.toonflow.ai.provider.ImageAiProvider;
import com.toonflow.ai.provider.TextAiProvider;
import com.toonflow.ai.provider.TtsAiProvider;
import com.toonflow.ai.provider.VideoAiProvider;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.AiModelMap;
import com.toonflow.entity.Config;
import com.toonflow.mapper.AiModelMapMapper;
import com.toonflow.mapper.ConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiProviderService {

    private final ConfigMapper configMapper;
    private final AiModelMapMapper aiModelMapMapper;
    private final TextAiProviderFactory textFactory;
    private final ImageAiProviderFactory imageFactory;
    private final VideoAiProviderFactory videoFactory;
    private final TtsAiProviderFactory ttsFactory;

    public TextAiProvider getTextProvider(String functionKey) {
        return textFactory.create(resolveConfig(functionKey));
    }

    public ImageAiProvider getImageProvider(String functionKey) {
        return imageFactory.create(resolveConfig(functionKey));
    }

    public VideoAiProvider getVideoProvider(String functionKey) {
        return videoFactory.create(resolveConfig(functionKey));
    }

    public TtsAiProvider getTtsProvider(String functionKey) {
        return ttsFactory.create(resolveConfig(functionKey));
    }

    private Config resolveConfig(String functionKey) {
        AiModelMap mapping = aiModelMapMapper.selectOne(
                new LambdaQueryWrapper<AiModelMap>().eq(AiModelMap::getKey, functionKey));
        if (mapping == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "未配置功能映射: " + functionKey);
        }
        Config config = configMapper.selectById(mapping.getConfigId());
        if (config == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模型配置不存在: configId=" + mapping.getConfigId());
        }
        return config;
    }
}
