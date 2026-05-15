package com.boylu.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boylu.entity.SysResource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boylu.vo.resource.SysResourceVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 资源表 Mapper接口
 */
@Mapper
public interface SysResourceMapper extends BaseMapper<SysResource> {

    Page<SysResourceVo> getResourceList(@Param("page") Page<Object> page, @Param("sysResource") SysResource sysResource);

}
