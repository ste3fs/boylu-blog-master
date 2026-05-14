package com.boylu.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.boylu.mapper.SysPhotoMapper;
import com.boylu.entity.SysPhoto;
import com.boylu.exception.ServiceException;
import com.boylu.service.SysPhotoService;
import com.boylu.utils.LocalFileUrlNormalizeUtil;
import com.boylu.utils.PageUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * 照片 服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysPhotoServiceImpl extends ServiceImpl<SysPhotoMapper, SysPhoto> implements SysPhotoService {

    /**
     * 查询照片分页列表
     */
    @Override
    public IPage<SysPhoto> selectPage(SysPhoto sysPhoto) {
        LambdaQueryWrapper<SysPhoto> wrapper = new LambdaQueryWrapper<>();
        // 构建查询条件
        wrapper.eq(sysPhoto.getId() != null, SysPhoto::getId, sysPhoto.getId());
        wrapper.eq(sysPhoto.getAlbumId() != null, SysPhoto::getAlbumId, sysPhoto.getAlbumId());
        wrapper.eq(sysPhoto.getDescription() != null, SysPhoto::getDescription, sysPhoto.getDescription());
        wrapper.eq(sysPhoto.getUrl() != null, SysPhoto::getUrl, sysPhoto.getUrl());
        wrapper.eq(sysPhoto.getRecordTime() != null, SysPhoto::getRecordTime, sysPhoto.getRecordTime());
        wrapper.eq(sysPhoto.getSort() != null, SysPhoto::getSort, sysPhoto.getSort());
        wrapper.eq(sysPhoto.getCreateTime() != null, SysPhoto::getCreateTime, sysPhoto.getCreateTime());
        return page(PageUtil.getPage(), wrapper);
    }

    /**
     * 查询照片列表
     */
    @Override
    public List<SysPhoto> selectList(SysPhoto sysPhoto) {
        LambdaQueryWrapper<SysPhoto> wrapper = new LambdaQueryWrapper<>();
        // 构建查询条件
        wrapper.eq(sysPhoto.getId() != null, SysPhoto::getId, sysPhoto.getId());
        wrapper.eq(sysPhoto.getAlbumId() != null, SysPhoto::getAlbumId, sysPhoto.getAlbumId());
        wrapper.eq(sysPhoto.getDescription() != null, SysPhoto::getDescription, sysPhoto.getDescription());
        wrapper.eq(sysPhoto.getUrl() != null, SysPhoto::getUrl, sysPhoto.getUrl());
        wrapper.eq(sysPhoto.getRecordTime() != null, SysPhoto::getRecordTime, sysPhoto.getRecordTime());
        wrapper.eq(sysPhoto.getSort() != null, SysPhoto::getSort, sysPhoto.getSort());
        wrapper.eq(sysPhoto.getCreateTime() != null, SysPhoto::getCreateTime, sysPhoto.getCreateTime());
        return list(wrapper);
    }

    /**
     * 新增照片
     */
    @Override
    public boolean insert(SysPhoto sysPhoto) {
        normalizePhotoUrl(sysPhoto);
        return save(sysPhoto);
    }

    /**
     * 修改照片
     */
    @Override
    public boolean update(SysPhoto sysPhoto) {
        normalizePhotoUrl(sysPhoto);
        return updateById(sysPhoto);
    }

    /**
     * 批量删除照片
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object move(List<Long> ids, Long albumId) {
        baseMapper.move(ids, albumId);
        return Boolean.TRUE;
    }

    private void normalizePhotoUrl(SysPhoto sysPhoto) {
        if (sysPhoto == null || StringUtils.isBlank(sysPhoto.getUrl())) {
            return;
        }

        String normalizedUrl = LocalFileUrlNormalizeUtil.normalizeUrl(sysPhoto.getUrl());
        if (StringUtils.startsWithIgnoreCase(normalizedUrl, "blob:")
                || StringUtils.startsWithIgnoreCase(normalizedUrl, "data:")) {
            throw new ServiceException("图片还在上传中，请等待上传完成后再保存");
        }
        sysPhoto.setUrl(normalizedUrl);
    }
}
