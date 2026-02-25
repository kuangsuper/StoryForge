package com.toonflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.toonflow.entity.TaskList;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskListMapper extends BaseMapper<TaskList> {
}
