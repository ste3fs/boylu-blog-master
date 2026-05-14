package com.boylu.service.impl;


import cn.dev33.satoken.stp.StpUtil;
import com.boylu.common.Constants;
import com.boylu.dto.feedback.SysFeedbackQueryDto;
import com.boylu.vo.feedback.SysFeedbackVo;
import org.springframework.stereotype.Service;
import com.boylu.mapper.SysFeedbackMapper;
import com.boylu.entity.SysFeedback;
import com.boylu.service.SysFeedbackService;
import com.boylu.utils.PageUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;

/**
 * 反馈表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysFeedbackServiceImpl extends ServiceImpl<SysFeedbackMapper, SysFeedback> implements SysFeedbackService {

    /**
     * 查询反馈表分页列表
     */
    @Override
    public IPage<SysFeedbackVo> selectPage(SysFeedbackQueryDto feedbackQueryDto) {
        // 门户端用户只能看自己的反馈，是否管理员必须以后端登录角色为准，不能信任请求参数。
        if (!StpUtil.hasRole(Constants.ADMIN)) {
            feedbackQueryDto.setUserId(StpUtil.getLoginIdAsLong());
        }
        return baseMapper.page(PageUtil.getPage(), feedbackQueryDto);
    }

    /**
     * 新增反馈表
     */
    @Override
    public boolean insert(SysFeedback sysFeedback) {
        sysFeedback.setUserId(StpUtil.getLoginIdAsLong());
        return save(sysFeedback);
    }

    /**
     * 修改反馈表
     */
    @Override
    public boolean update(SysFeedback sysFeedback) {
        return updateById(sysFeedback);
    }
}
