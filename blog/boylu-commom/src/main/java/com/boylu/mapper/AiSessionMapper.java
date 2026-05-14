package com.boylu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boylu.entity.AiSession;
import com.boylu.vo.ai.AiSessionListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiSessionMapper extends BaseMapper<AiSession> {

    IPage<AiSessionListVo> selectSessionPage(@Param("page") Page<Object> page, @Param("userId") Long userId);
}
