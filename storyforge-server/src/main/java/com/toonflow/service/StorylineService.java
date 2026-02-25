package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.dto.request.SaveStorylineRequest;
import com.toonflow.entity.Storyline;
import com.toonflow.mapper.StorylineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorylineService {

    private final StorylineMapper storylineMapper;

    public Storyline get(Long projectId) {
        return storylineMapper.selectOne(
                new LambdaQueryWrapper<Storyline>().eq(Storyline::getProjectId, projectId));
    }

    public void saveOrUpdate(Long projectId, SaveStorylineRequest request) {
        Storyline existing = get(projectId);
        if (existing == null) {
            Storyline s = new Storyline();
            s.setProjectId(projectId);
            s.setName(request.getName());
            s.setContent(request.getContent());
            s.setNovelIds(request.getNovelIds());
            storylineMapper.insert(s);
        } else {
            if (request.getName() != null) existing.setName(request.getName());
            if (request.getContent() != null) existing.setContent(request.getContent());
            if (request.getNovelIds() != null) existing.setNovelIds(request.getNovelIds());
            storylineMapper.updateById(existing);
        }
    }

    public void delete(Long projectId) {
        storylineMapper.delete(
                new LambdaQueryWrapper<Storyline>().eq(Storyline::getProjectId, projectId));
    }
}
