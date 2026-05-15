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
import org.apache.commons.lang3.StringUtils;
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

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_CATEGORY_LENGTH = 64;
    private static final int MAX_COVER_LENGTH = 512;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_PAN_PATH_LENGTH = 255;
    private static final int MAX_PAN_CODE_LENGTH = 64;

    private final SysResourceMapper baseMapper;

    private final RedisUtil redisUtil;

    @Override
    public Page<SysResourceVo> getResourceList(SysResource sysResource) {
        return baseMapper.getResourceList(PageUtil.getPage(),sysResource);
    }

    @Override
    public void add(SysResource sysResource) {
        normalizeAndValidate(sysResource);
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

    @Override
    public SysResource download(Long id) {
        if (id == null || id <= 0) {
            throw new ServiceException("资源不存在");
        }

        SysResource sysResource = baseMapper.selectById(id);
        if (sysResource == null || !ResourceStatusEnum.PASS.getCode().equals(sysResource.getStatus())) {
            throw new ServiceException("资源不存在或未通过审核");
        }
        if (!Integer.valueOf(1).equals(sysResource.getIsFree())) {
            throw new ServiceException("付费资源请完成验证后下载");
        }

        sysResource.setDownloads((sysResource.getDownloads() == null ? 0 : sysResource.getDownloads()) + 1);
        baseMapper.updateById(sysResource);

        return sysResource;
    }

    private void normalizeAndValidate(SysResource sysResource) {
        if (sysResource == null) {
            throw new ServiceException("资源信息不能为空");
        }

        sysResource.setName(StringUtils.trimToEmpty(sysResource.getName()));
        sysResource.setCategory(StringUtils.trimToEmpty(sysResource.getCategory()));
        sysResource.setCover(StringUtils.trimToNull(sysResource.getCover()));
        sysResource.setDescription(StringUtils.trimToNull(sysResource.getDescription()));
        sysResource.setPanPath(StringUtils.trimToEmpty(sysResource.getPanPath()));
        sysResource.setPanCode(StringUtils.trimToNull(sysResource.getPanCode()));

        if (StringUtils.isBlank(sysResource.getName())) {
            throw new ServiceException("资源名称不能为空");
        }
        if (sysResource.getName().length() > MAX_NAME_LENGTH) {
            throw new ServiceException("资源名称不能超过 50 个字符");
        }
        if (StringUtils.isBlank(sysResource.getCategory())) {
            throw new ServiceException("资源分类不能为空");
        }
        if (sysResource.getCategory().length() > MAX_CATEGORY_LENGTH) {
            throw new ServiceException("资源分类过长");
        }
        if (StringUtils.isNotBlank(sysResource.getCover()) && sysResource.getCover().length() > MAX_COVER_LENGTH) {
            throw new ServiceException("资源封面地址过长");
        }
        if (StringUtils.isNotBlank(sysResource.getDescription()) && sysResource.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new ServiceException("资源描述不能超过 500 个字符");
        }
        if (StringUtils.isBlank(sysResource.getPanPath())) {
            throw new ServiceException("网盘地址不能为空");
        }
        if (sysResource.getPanPath().length() > MAX_PAN_PATH_LENGTH) {
            throw new ServiceException("网盘地址过长");
        }
        if (StringUtils.isNotBlank(sysResource.getPanCode()) && sysResource.getPanCode().length() > MAX_PAN_CODE_LENGTH) {
            throw new ServiceException("提取码过长");
        }

        Integer isFree = sysResource.getIsFree();
        if (isFree == null) {
            sysResource.setIsFree(1);
        } else if (!Integer.valueOf(0).equals(isFree) && !Integer.valueOf(1).equals(isFree)) {
            throw new ServiceException("资源类型不正确");
        }
    }
}
