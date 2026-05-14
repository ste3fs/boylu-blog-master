package com.boylu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.boylu.dto.article.ArticleQueryDto;
import com.boylu.entity.SysArticle;
import com.boylu.vo.article.ArticleListVo;
import com.boylu.vo.article.SysArticleDetailVo;

import java.util.List;

public interface SysArticleService extends IService<SysArticle> {

    /**
     * 分页查询
     * @param articleQueryDto
     * @return
     */
    IPage<ArticleListVo> selectPage(ArticleQueryDto articleQueryDto);

    /**
     * 文章详情
     * @param id
     * @return
     */
    SysArticleDetailVo detail(Integer id);

    /**
     * 新增
     * @param sysArticle
     * @return
     */
    Boolean add(SysArticleDetailVo sysArticle);

    /**
     * 修改
     * @param sysArticle
     * @return
     */
    Boolean update(SysArticleDetailVo sysArticle);


    /**
     * 删除
     * @param ids
     * @return
     */
    Boolean delete(List<Long> ids);

    /**
     * 爬取文章
     * @param url
     */
    void reptile(String url);
}
