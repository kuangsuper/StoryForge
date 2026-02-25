package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.dto.request.SaveOutlineRequest;
import com.toonflow.entity.Assets;
import com.toonflow.entity.Outline;
import com.toonflow.entity.Script;
import com.toonflow.entity.json.AssetItem;
import com.toonflow.entity.json.EpisodeData;
import com.toonflow.mapper.AssetsMapper;
import com.toonflow.mapper.OutlineMapper;
import com.toonflow.mapper.ScriptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OutlineService {

    private final OutlineMapper outlineMapper;
    private final ScriptMapper scriptMapper;
    private final AssetsMapper assetsMapper;

    public List<Outline> list(Long projectId, String mode) {
        LambdaQueryWrapper<Outline> query = new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .orderByAsc(Outline::getEpisode);
        if ("simple".equals(mode)) {
            query.select(Outline::getId, Outline::getProjectId, Outline::getEpisode);
        }
        return outlineMapper.selectList(query);
    }

    public Outline create(Long projectId, SaveOutlineRequest request) {
        Outline outline = new Outline();
        outline.setProjectId(projectId);
        outline.setEpisode(request.getEpisode());
        outline.setData(request.getData());
        outlineMapper.insert(outline);
        return outline;
    }

    public void update(Long id, SaveOutlineRequest request) {
        Outline outline = outlineMapper.selectById(id);
        if (outline == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (request.getEpisode() != null) outline.setEpisode(request.getEpisode());
        if (request.getData() != null) outline.setData(request.getData());
        outlineMapper.updateById(outline);
    }

    @Transactional
    public void batchDelete(Long projectId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        outlineMapper.deleteBatchIds(ids);
        for (Long outlineId : ids) {
            scriptMapper.delete(new LambdaQueryWrapper<Script>()
                    .eq(Script::getOutlineId, outlineId));
        }
    }

    @Transactional
    public void extractAssets(Long projectId) {
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
            if (!map.containsKey(key)) {
                Assets a = new Assets();
                a.setProjectId(projectId);
                a.setName(item.getName());
                a.setIntro(item.getDescription());
                a.setType(type);
                map.put(key, a);
            }
        }
    }
}
