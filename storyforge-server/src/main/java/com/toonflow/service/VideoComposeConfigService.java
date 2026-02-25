package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.VideoComposeConfig;
import com.toonflow.mapper.VideoComposeConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoComposeConfigService {

    private final VideoComposeConfigMapper videoComposeConfigMapper;

    public List<VideoComposeConfig> list(Long projectId) {
        return videoComposeConfigMapper.selectList(
                new LambdaQueryWrapper<VideoComposeConfig>().eq(VideoComposeConfig::getProjectId, projectId));
    }

    public VideoComposeConfig getById(Long id) {
        VideoComposeConfig config = videoComposeConfigMapper.selectById(id);
        if (config == null) throw new BizException(ErrorCode.NOT_FOUND);
        return config;
    }

    public VideoComposeConfig save(Long projectId, VideoComposeConfig config) {
        config.setProjectId(projectId);
        if (config.getId() != null) {
            videoComposeConfigMapper.updateById(config);
        } else {
            videoComposeConfigMapper.insert(config);
        }
        return config;
    }

    public void delete(Long id) {
        videoComposeConfigMapper.deleteById(id);
    }
}
