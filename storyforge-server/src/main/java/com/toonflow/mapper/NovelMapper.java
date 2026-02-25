package com.toonflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.toonflow.entity.Novel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NovelMapper extends BaseMapper<Novel> {
}
