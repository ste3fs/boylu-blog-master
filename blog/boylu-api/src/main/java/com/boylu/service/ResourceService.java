package com.boylu.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boylu.entity.SysResource;
import com.boylu.vo.resource.SysResourceVo;

/**
 * @author: boylu
 * @date: 2025/3/12
 * @description:
 */
public interface ResourceService {

    Page<SysResourceVo> getResourceList(SysResource sysResource);

    void add(SysResource sysResource);

    SysResource verify(String code,Long id);

    SysResource download(Long id);
}
