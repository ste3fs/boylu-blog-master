package com.boylu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boylu.vo.article.CategoryListVo;
import com.boylu.entity.SysCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


/**
 * 分类 Mapper接口
 */
@Mapper
public interface SysCategoryMapper extends BaseMapper<SysCategory> {
    List<CategoryListVo> getArticleCategories();

}