package com.boylu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boylu.entity.SysWebConfig;

public interface SysWebConfigService extends IService<SysWebConfig> {

    void update(SysWebConfig sysWebConfig);
}
