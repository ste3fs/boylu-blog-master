package com.boylu.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boylu.common.RedisConstants;
import com.boylu.entity.SysResource;
import com.boylu.enums.ResourceStatusEnum;
import com.boylu.exception.ServiceException;
import com.boylu.mapper.SysResourceMapper;
import com.boylu.service.ResourceService;
import com.boylu.utils.PageUtil;
import com.boylu.utils.RedisUtil;
import com.boylu.vo.resource.SysResourceVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author: boylu
 * @date: 2025/3/12
 * @description:
 */
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final SysResourceMapper baseMapper;

    private final RedisUtil redisUtil;

    @Override
    public Page<SysResourceVo> getResourceList(SysResource sysResource) {
        return baseMapper.getResourceList(PageUtil.getPage(),sysResource);
    }

    @Override
    public void add(SysResource sysResource) {
        sysResource.setUserId(StpUtil.getLoginIdAsLong());
        sysResource.setStatus(ResourceStatusEnum.AUDIT.getCode());
        baseMapper.insert(sysResource);
    }

    @Override
    public SysResource verify(String code,Long id) {
        String key = RedisConstants.CAPTCHA_CODE_KEY + code;
        if (!redisUtil.hasKey(key)) {
            throw new ServiceException("验证码错误");
        }
        redisUtil.delete(key);

        SysResource sysResource = baseMapper.selectById(id);

        sysResource.setDownloads(sysResource.getDownloads() + 1);
        baseMapper.updateById(sysResource);

        return sysResource;
    }
}
