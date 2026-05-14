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
        sysResource.setDownloads(0);
        baseMapper.insert(sysResource);
    }

    @Override
    public SysResource verify(String code,Long id) {
        if (id == null || id <= 0) {
            throw new ServiceException("资源不存在");
        }
        String key = RedisConstants.CAPTCHA_CODE_KEY + code;
        if (!redisUtil.hasKey(key)) {
            throw new ServiceException("验证码错误");
        }
        redisUtil.delete(key);

        SysResource sysResource = baseMapper.selectById(id);
        if (sysResource == null || !ResourceStatusEnum.PASS.getCode().equals(sysResource.getStatus())) {
            throw new ServiceException("资源不存在或未通过审核");
        }

        sysResource.setDownloads((sysResource.getDownloads() == null ? 0 : sysResource.getDownloads()) + 1);
        baseMapper.updateById(sysResource);

        return sysResource;
    }
}
