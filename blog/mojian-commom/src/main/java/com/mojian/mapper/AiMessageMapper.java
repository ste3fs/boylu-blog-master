package com.mojian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mojian.entity.AiMessage;
import com.mojian.vo.ai.AiMessageVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {

    List<AiMessageVo> selectMessagesBySessionId(@Param("sessionId") Long sessionId);

    List<AiMessageVo> selectRecentMessages(@Param("sessionId") Long sessionId, @Param("limit") Integer limit);
}
