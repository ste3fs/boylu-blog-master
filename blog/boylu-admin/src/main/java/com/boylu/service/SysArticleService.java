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
     * 手动推送单篇文章URL到百度
     * @param articleId 文章ID
     * @return 是否已触发推送
     */
    Boolean pushToBaidu(Long articleId);

    /**
     * 手动批量推送最近发布文章URL到百度
     * @param limit 推送数量
     * @return 实际触发数量
     */
    Integer pushRecentPublishedToBaidu(Integer limit);

    /**
     * 爬取文章
     * @param url
     */
    void reptile(String url);
}
