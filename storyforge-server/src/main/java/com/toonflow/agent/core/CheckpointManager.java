package com.toonflow.agent.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toonflow.entity.TaskList;
import com.toonflow.mapper.TaskListMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckpointManager {

    private final TaskListMapper taskListMapper;
    private final ObjectMapper objectMapper;

    public void save(Long projectId, String agentType, Checkpoint checkpoint) {
        try {
            String json = objectMapper.writeValueAsString(checkpoint);
            TaskList existing = findGenerating(projectId, agentType);
            if (existing != null) {
                existing.setCheckpoint(json);
                taskListMapper.updateById(existing);
            } else {
                TaskList task = new TaskList();
                task.setProjectId(projectId);
                task.setName(agentType);
                task.setState("generating");
                task.setCheckpoint(json);
                taskListMapper.insert(task);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize checkpoint for project={} agent={}", projectId, agentType, e);
        }
    }

    public Checkpoint load(Long projectId, String agentType) {
        TaskList task = findGenerating(projectId, agentType);
        if (task == null || task.getCheckpoint() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(task.getCheckpoint(), Checkpoint.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize checkpoint for project={} agent={}", projectId, agentType, e);
            return null;
        }
    }

    public boolean hasUnfinished(Long projectId, String agentType) {
        return findGenerating(projectId, agentType) != null;
    }

    private TaskList findGenerating(Long projectId, String agentType) {
        return taskListMapper.selectOne(
                new LambdaQueryWrapper<TaskList>()
                        .eq(TaskList::getProjectId, projectId)
                        .eq(TaskList::getName, agentType)
                        .eq(TaskList::getState, "generating")
                        .orderByDesc(TaskList::getId)
                        .last("LIMIT 1"));
    }
}
