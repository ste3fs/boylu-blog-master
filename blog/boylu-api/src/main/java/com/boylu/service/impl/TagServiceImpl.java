package com.boylu.service.impl;

import com.boylu.service.TagService;
import com.boylu.vo.tag.TagListVo;
import com.boylu.mapper.SysTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final SysTagMapper sysTagMapper;

    @Override
    public List<TagListVo> getTagsApi() {
        return sysTagMapper.getTagsApi();
    }
}
