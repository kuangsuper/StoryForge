package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.ai.factory.ImageAiProviderFactory;
import com.toonflow.ai.factory.TextAiProviderFactory;
import com.toonflow.ai.factory.VideoAiProviderFactory;
import com.toonflow.ai.model.AiRequest;
import com.toonflow.ai.model.ChatMessage;
import com.toonflow.ai.model.ImageRequest;
import com.toonflow.ai.model.TestResult;
import com.toonflow.ai.model.VideoRequest;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.dto.request.SaveConfigRequest;
import com.toonflow.dto.request.SaveModelMapRequest;
import com.toonflow.dto.response.ConfigResponse;
import com.toonflow.entity.AiModelMap;
import com.toonflow.entity.Config;
import com.toonflow.mapper.AiModelMapMapper;
import com.toonflow.mapper.ConfigMapper;
import com.toonflow.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigMapper configMapper;
    private final AiModelMapMapper aiModelMapMapper;
    private final TextAiProviderFactory textAiProviderFactory;
    private final ImageAiProviderFactory imageAiProviderFactory;
    private final VideoAiProviderFactory videoAiProviderFactory;

    public void create(SaveConfigRequest request) {
        Config config = new Config();
        config.setType(request.getType());
        config.setName(request.getName());
        config.setManufacturer(request.getManufacturer());
        config.setModel(request.getModel());
        config.setApiKey(AesUtil.encrypt(request.getApiKey()));
        config.setBaseUrl(request.getBaseUrl());
        configMapper.insert(config);
    }

    public List<ConfigResponse> list() {
        return configMapper.selectList(null).stream().map(this::toResponse).toList();
    }

    public void update(Long id, SaveConfigRequest request) {
        Config config = configMapper.selectById(id);
        if (config == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (request.getType() != null) config.setType(request.getType());
        if (request.getName() != null) config.setName(request.getName());
        if (request.getManufacturer() != null) config.setManufacturer(request.getManufacturer());
        if (request.getModel() != null) config.setModel(request.getModel());
        if (request.getApiKey() != null) config.setApiKey(AesUtil.encrypt(request.getApiKey()));
        if (request.getBaseUrl() != null) config.setBaseUrl(request.getBaseUrl());
        configMapper.updateById(config);
    }

    public void delete(Long id) {
        if (configMapper.selectById(id) == null) throw new BizException(ErrorCode.NOT_FOUND);
        configMapper.deleteById(id);
        aiModelMapMapper.delete(
                new LambdaQueryWrapper<AiModelMap>().eq(AiModelMap::getConfigId, id));
    }

    public List<AiModelMap> getModelMaps() {
        return aiModelMapMapper.selectList(null);
    }

    @Transactional
    public void updateModelMaps(SaveModelMapRequest request) {
        aiModelMapMapper.delete(null);
        if (request.getMappings() != null) {
            for (SaveModelMapRequest.ModelMapItem item : request.getMappings()) {
                AiModelMap map = new AiModelMap();
                map.setKey(item.getKey());
                map.setName(item.getName());
                map.setConfigId(item.getConfigId());
                aiModelMapMapper.insert(map);
            }
        }
    }

    public TestResult testConnectivity(Long id, String type) {
        Config config = configMapper.selectById(id);
        if (config == null) throw new BizException(ErrorCode.NOT_FOUND);
        long start = System.currentTimeMillis();
        try {
            switch (type) {
                case "text" -> {
                    var provider = textAiProviderFactory.create(config);
                    var request = AiRequest.builder()
                            .messages(List.of(ChatMessage.builder().role("user").content("Hi").build()))
                            .maxTokens(10)
                            .build();
                    provider.invoke(request);
                }
                case "image" -> {
                    var provider = imageAiProviderFactory.create(config);
                    provider.generate(ImageRequest.builder().prompt("test").width(64).height(64).build());
                }
                case "video" -> {
                    var provider = videoAiProviderFactory.create(config);
                    provider.createTask(VideoRequest.builder().prompt("test").build());
                }
                default -> throw new BizException(ErrorCode.BAD_REQUEST, "不支持的测试类型: " + type);
            }
            long elapsed = System.currentTimeMillis() - start;
            return TestResult.builder().success(true).message("连通成功").responseTimeMs(elapsed).build();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return TestResult.builder().success(false)
                    .message("连通失败: " + e.getMessage()).responseTimeMs(elapsed).build();
        }
    }

    private ConfigResponse toResponse(Config config) {
        ConfigResponse resp = new ConfigResponse();
        resp.setId(config.getId());
        resp.setType(config.getType());
        resp.setName(config.getName());
        resp.setManufacturer(config.getManufacturer());
        resp.setModel(config.getModel());
        resp.setBaseUrl(config.getBaseUrl());
        try {
            String decrypted = AesUtil.decrypt(config.getApiKey());
            resp.setMaskedApiKey(AesUtil.maskApiKey(decrypted));
        } catch (Exception e) {
            resp.setMaskedApiKey("****");
        }
        return resp;
    }
}
