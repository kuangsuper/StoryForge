package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.dto.request.AddNovelRequest;
import com.toonflow.dto.request.UpdateNovelRequest;
import com.toonflow.dto.response.NovelListResponse;
import com.toonflow.entity.Novel;
import com.toonflow.mapper.NovelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelMapper novelMapper;

    public void batchCreate(Long projectId, AddNovelRequest request) {
        for (AddNovelRequest.NovelChapterItem item : request.getChapters()) {
            Novel novel = new Novel();
            novel.setProjectId(projectId);
            novel.setChapterIndex(item.getChapterIndex());
            novel.setReel(item.getReel());
            novel.setChapter(item.getChapter());
            novel.setChapterData(item.getChapterData());
            novelMapper.insert(novel);
        }
    }

    public List<NovelListResponse> list(Long projectId) {
        List<Novel> novels = novelMapper.selectList(
                new LambdaQueryWrapper<Novel>()
                        .eq(Novel::getProjectId, projectId)
                        .select(Novel::getId, Novel::getChapterIndex, Novel::getVolumeIndex,
                                Novel::getReel, Novel::getChapter, Novel::getCreateTime)
                        .orderByAsc(Novel::getChapterIndex));
        return novels.stream().map(n -> {
            NovelListResponse r = new NovelListResponse();
            r.setId(n.getId());
            r.setChapterIndex(n.getChapterIndex());
            r.setVolumeIndex(n.getVolumeIndex());
            r.setReel(n.getReel());
            r.setChapter(n.getChapter());
            r.setCreateTime(n.getCreateTime());
            return r;
        }).toList();
    }

    public Novel getById(Long id) {
        Novel novel = novelMapper.selectById(id);
        if (novel == null) throw new BizException(ErrorCode.NOT_FOUND);
        return novel;
    }

    public void update(Long id, UpdateNovelRequest request) {
        Novel novel = getById(id);
        if (request.getChapter() != null) novel.setChapter(request.getChapter());
        if (request.getChapterData() != null) novel.setChapterData(request.getChapterData());
        if (request.getReel() != null) novel.setReel(request.getReel());
        novelMapper.updateById(novel);
    }

    public void delete(Long id) {
        if (novelMapper.selectById(id) == null) throw new BizException(ErrorCode.NOT_FOUND);
        novelMapper.deleteById(id);
    }
}
