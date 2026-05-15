package com.boylu.service.impl;

import com.boylu.common.RedisConstants;
import com.boylu.vo.dashboard.ContributionData;
import com.boylu.vo.dashboard.IndexVo;
import com.boylu.mapper.SysArticleMapper;
import com.boylu.mapper.SysMessageMapper;
import com.boylu.mapper.SysUserMapper;
import com.boylu.service.IndexService;
import com.boylu.utils.RedisUtil;
import com.boylu.vo.dashboard.VisitTrendData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final SysUserMapper sysUserMapper;

    private final SysArticleMapper sysArticleMapper;

    private final SysMessageMapper sysMessageMapper;

    private final RedisUtil redisUtil;

    @Override
    public IndexVo index() {
        Long userCount = sysUserMapper.selectCount(null);
        Long articleCount = sysArticleMapper.selectCount(null);
        Long messageCount = sysMessageMapper.selectCount(null);

        long visitCount = readLong(RedisConstants.BLOG_VIEWS_COUNT);

        List<ContributionData> list = sysArticleMapper.getThisYearContributionData();

        return IndexVo.builder()
                .articleCount(articleCount)
                .userCount(userCount)
                .messageCount(messageCount)
                .visitCount(visitCount)
                .contributionData(list)
                .visitTrendData(buildVisitTrendData())
                .build();
    }

    @Override
    public List<Map<String, Integer>> getCategories() {
        List<Map<String, Integer>> list = sysArticleMapper.selectCountByCategory();
        return list;
    }

    private List<VisitTrendData> buildVisitTrendData() {
        List<VisitTrendData> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter keyFormatter = DateTimeFormatter.ISO_DATE;
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateKey = date.format(keyFormatter);
            result.add(VisitTrendData.builder()
                    .date(dateKey)
                    .label(date.format(labelFormatter))
                    .visitCount(readLong(RedisConstants.UNIQUE_VISITOR_DAILY + dateKey))
                    .viewCount(readLong(RedisConstants.BLOG_VIEWS_DAILY + dateKey))
                    .build());
        }

        return result;
    }

    private long readLong(String key) {
        try {
            if (!Boolean.TRUE.equals(redisUtil.hasKey(key))) {
                return 0L;
            }

            String type = redisUtil.type(key);
            if (!"string".equalsIgnoreCase(type)) {
                return 0L;
            }

            Object value = redisUtil.get(key);
            if (value == null) {
                return 0L;
            }
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0L;
        }
    }
}
