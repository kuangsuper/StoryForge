package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.VideoConfig;
import com.toonflow.mapper.VideoConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoConfigService {

    private final VideoConfigMapper videoConfigMapper;

    public List<VideoConfig> list(Long projectId) {
        return videoConfigMapper.selectList(
                new LambdaQueryWrapper<VideoConfig>().eq(VideoConfig::getProjectId, projectId));
    }

    public VideoConfig getById(Long id) {
        VideoConfig config = videoConfigMapper.selectById(id);
        if (config == null) throw new BizException(ErrorCode.NOT_FOUND);
        return config;
    }

    public VideoConfig save(Long projectId, VideoConfig config) {
        config.setProjectId(projectId);
        if (config.getId() != null) {
            videoConfigMapper.updateById(config);
        } else {
            videoConfigMapper.insert(config);
        }
        return config;
    }

    public void delete(Long id) {
        videoConfigMapper.deleteById(id);
    }
}
