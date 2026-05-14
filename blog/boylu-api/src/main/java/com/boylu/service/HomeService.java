package com.boylu.service;

import com.alibaba.fastjson2.JSONObject;
import com.boylu.common.Result;
import com.boylu.entity.SysNotice;
import com.boylu.entity.SysWebConfig;

import java.util.List;
import java.util.Map;

public interface HomeService {

    /**
     * 获取网站配置
     * @return
     */
    Result<SysWebConfig> getWebConfig();

    /**
     * 获取热搜
     * @param type
     * @return
     */
    JSONObject getHotSearch(String type);

    /**
     * 添加访问量
     * @return
     */
    void report();


    /**
     * 获取公告
     * @return
     */
    Map<String, List<SysNotice>> getNotice();

}
