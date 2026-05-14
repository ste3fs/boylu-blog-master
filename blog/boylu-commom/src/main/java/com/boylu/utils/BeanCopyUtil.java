package com.boylu.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: boylu
 * @date: 2025/2/25
 * @description:
 */
public class BeanCopyUtil {

    private static final Logger log = LoggerFactory.getLogger(BeanCopyUtil.class);

    /**
     * 对象拷贝
     * @param source
     * @param targetClass
     * @return
     * @param <S>
     * @param <T>
     */
    public static <S, T> T copyObj(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        T target = null;
        try {
            target = targetClass.newInstance();
            org.springframework.beans.BeanUtils.copyProperties(source, target);
        } catch (Exception e) {
            log.error("对象拷贝失败，targetClass={}", targetClass.getName(), e);
        }
        return target;
    }

}
