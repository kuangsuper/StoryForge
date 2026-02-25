package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.TtsConfig;
import com.toonflow.mapper.TtsConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TtsConfigService {

    private final TtsConfigMapper ttsConfigMapper;

    public List<TtsConfig> list(Long projectId) {
        return ttsConfigMapper.selectList(
                new LambdaQueryWrapper<TtsConfig>().eq(TtsConfig::getProjectId, projectId));
    }

    public TtsConfig create(Long projectId, TtsConfig config) {
        config.setProjectId(projectId);
        ttsConfigMapper.insert(config);
        return config;
    }

    public void update(Long id, TtsConfig config) {
        TtsConfig existing = ttsConfigMapper.selectById(id);
        if (existing == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (config.getVoiceId() != null) existing.setVoiceId(config.getVoiceId());
        if (config.getManufacturer() != null) existing.setManufacturer(config.getManufacturer());
        if (config.getSpeed() != null) existing.setSpeed(config.getSpeed());
        if (config.getPitch() != null) existing.setPitch(config.getPitch());
        if (config.getEmotion() != null) existing.setEmotion(config.getEmotion());
        ttsConfigMapper.updateById(existing);
    }

    public void delete(Long id) {
        ttsConfigMapper.deleteById(id);
    }
}
