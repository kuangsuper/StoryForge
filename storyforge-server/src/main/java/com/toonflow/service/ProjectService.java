package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.common.PageResult;
import com.toonflow.dto.request.CreateProjectRequest;
import com.toonflow.dto.request.UpdateProjectRequest;
import com.toonflow.entity.*;
import com.toonflow.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final NovelMapper novelMapper;
    private final StorylineMapper storylineMapper;
    private final OutlineMapper outlineMapper;
    private final ScriptMapper scriptMapper;
    private final AssetsMapper assetsMapper;
    private final ImageMapper imageMapper;
    private final VideoMapper videoMapper;
    private final VideoComposeMapper videoComposeMapper;
    private final VideoComposeConfigMapper videoComposeConfigMapper;
    private final VideoConfigMapper videoConfigMapper;
    private final TtsAudioMapper ttsAudioMapper;
    private final TtsConfigMapper ttsConfigMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final AgentLogMapper agentLogMapper;
    private final TaskListMapper taskListMapper;

    public Project create(Long userId, CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setIntro(request.getIntro());
        project.setType(request.getType());
        project.setArtStyle(request.getArtStyle());
        project.setVideoRatio(request.getVideoRatio());
        project.setUserId(userId);
        projectMapper.insert(project);
        return project;
    }

    public PageResult<Project> list(Long userId, int page, int size) {
        Page<Project> p = projectMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Project>().eq(Project::getUserId, userId)
                        .orderByDesc(Project::getCreateTime));
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public Project getById(Long id, Long userId) {
        Project project = projectMapper.selectById(id);
        if (project == null) throw new BizException(ErrorCode.NOT_FOUND);
        if (!project.getUserId().equals(userId)) throw new BizException(ErrorCode.FORBIDDEN);
        return project;
    }

    public void update(Long id, Long userId, UpdateProjectRequest request) {
        Project project = getById(id, userId);
        if (request.getName() != null) project.setName(request.getName());
        if (request.getIntro() != null) project.setIntro(request.getIntro());
        if (request.getType() != null) project.setType(request.getType());
        if (request.getArtStyle() != null) project.setArtStyle(request.getArtStyle());
        if (request.getVideoRatio() != null) project.setVideoRatio(request.getVideoRatio());
        projectMapper.updateById(project);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        getById(id, userId);
        LambdaQueryWrapper<Novel> novelQ = new LambdaQueryWrapper<Novel>().eq(Novel::getProjectId, id);
        LambdaQueryWrapper<Storyline> storylineQ = new LambdaQueryWrapper<Storyline>().eq(Storyline::getProjectId, id);
        LambdaQueryWrapper<Outline> outlineQ = new LambdaQueryWrapper<Outline>().eq(Outline::getProjectId, id);
        LambdaQueryWrapper<Script> scriptQ = new LambdaQueryWrapper<Script>().eq(Script::getProjectId, id);
        LambdaQueryWrapper<Assets> assetsQ = new LambdaQueryWrapper<Assets>().eq(Assets::getProjectId, id);
        LambdaQueryWrapper<Image> imageQ = new LambdaQueryWrapper<Image>().eq(Image::getProjectId, id);
        LambdaQueryWrapper<Video> videoQ = new LambdaQueryWrapper<Video>().eq(Video::getProjectId, id);

        // 清理关联数据（之前遗漏的表）
        videoComposeMapper.delete(new LambdaQueryWrapper<VideoCompose>().eq(VideoCompose::getProjectId, id));
        videoComposeConfigMapper.delete(new LambdaQueryWrapper<VideoComposeConfig>().eq(VideoComposeConfig::getProjectId, id));
        videoConfigMapper.delete(new LambdaQueryWrapper<VideoConfig>().eq(VideoConfig::getProjectId, id));
        ttsAudioMapper.delete(new LambdaQueryWrapper<TtsAudio>().eq(TtsAudio::getProjectId, id));
        ttsConfigMapper.delete(new LambdaQueryWrapper<TtsConfig>().eq(TtsConfig::getProjectId, id));
        chatHistoryMapper.delete(new LambdaQueryWrapper<ChatHistory>().eq(ChatHistory::getProjectId, id));
        agentLogMapper.delete(new LambdaQueryWrapper<AgentLog>().eq(AgentLog::getProjectId, id));
        taskListMapper.delete(new LambdaQueryWrapper<TaskList>().eq(TaskList::getProjectId, id));

        novelMapper.delete(novelQ);
        storylineMapper.delete(storylineQ);
        outlineMapper.delete(outlineQ);
        scriptMapper.delete(scriptQ);
        assetsMapper.delete(assetsQ);
        imageMapper.delete(imageQ);
        videoMapper.delete(videoQ);
        projectMapper.deleteById(id);
    }

    public long stats(Long userId) {
        return projectMapper.selectCount(
                new LambdaQueryWrapper<Project>().eq(Project::getUserId, userId));
    }
}
