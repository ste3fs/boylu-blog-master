package com.boylu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boylu.entity.SysMoment;
import com.boylu.vo.moment.MomentPageVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: boylu
 * @date: 2025/2/5
 * @description:
 */
@Mapper
public interface SysMomentMapper extends BaseMapper<SysMoment> {


    IPage<MomentPageVo> selectPage(IPage<SysMoment> page);
}
