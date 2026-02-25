package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.common.PageResult;
import com.toonflow.entity.TaskList;
import com.toonflow.mapper.TaskListMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskListMapper taskListMapper;

    public PageResult<TaskList> list(String state, int page, int size) {
        LambdaQueryWrapper<TaskList> query = new LambdaQueryWrapper<TaskList>()
                .orderByDesc(TaskList::getCreateTime);
        if (state != null && !state.isBlank()) {
            query.eq(TaskList::getState, state);
        }
        Page<TaskList> pageResult = taskListMapper.selectPage(new Page<>(page, size), query);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(),
                (int) pageResult.getCurrent(), (int) pageResult.getSize());
    }

    public TaskList getById(Long id) {
        TaskList task = taskListMapper.selectById(id);
        if (task == null) throw new BizException(ErrorCode.NOT_FOUND);
        return task;
    }
}
