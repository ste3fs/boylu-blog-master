package com.mojian.utils;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author: boylu
 * @date 2021/12/8
 * @apiNote
 */
@Component
public class SpringUtil implements ApplicationContextAware {
    /** Spring搴旂敤涓婁笅鏂囩幆澧?     * -- GETTER --
     *  鑾峰彇applicationContext
     *
     * @return applicationContext
     */
    @Getter
    private static ApplicationContext applicationContext;


    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException
    {
        SpringUtil.applicationContext = applicationContext;
    }


    /**
     * 鑾峰彇瀵硅薄
     *
     * @param name
     * @return Object 涓€涓互鎵€缁欏悕瀛楁敞鍐岀殑bean鐨勫疄渚?     * @throws BeansException
     *
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException
    {
        return (T) getApplicationContext().getBean(name);
    }

    /**
     * 鑾峰彇绫诲瀷涓簉equiredType鐨勫璞?     *
     * @param clz
     * @return
     * @throws BeansException
     *
     */
    public static <T> T getBean(Class<T> clz) throws BeansException
    {
        T result = (T) getApplicationContext().getBean(clz);
        return result;
    }

    /**
     * 濡傛灉BeanFactory鍖呭惈涓€涓笌鎵€缁欏悕绉板尮閰嶇殑bean瀹氫箟锛屽垯杩斿洖true
     *
     * @param name
     * @return boolean
     */
    public static boolean containsBean(String name)
    {
        return getApplicationContext().containsBean(name);
    }

    /**
     * 鍒ゆ柇浠ョ粰瀹氬悕瀛楁敞鍐岀殑bean瀹氫箟鏄竴涓猻ingleton杩樻槸涓€涓猵rototype銆?濡傛灉涓庣粰瀹氬悕瀛楃浉搴旂殑bean瀹氫箟娌℃湁琚壘鍒帮紝灏嗕細鎶涘嚭涓€涓紓甯革紙NoSuchBeanDefinitionException锛?     *
     * @param name
     * @return boolean
     * @throws NoSuchBeanDefinitionException
     *
     */
    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException
    {
        return getApplicationContext().isSingleton(name);
    }

    /**
     * @param name
     * @return Class 娉ㄥ唽瀵硅薄鐨勭被鍨?     * @throws NoSuchBeanDefinitionException
     *
     */
    public static Class<?> getType(String name) throws NoSuchBeanDefinitionException
    {
        return getApplicationContext().getType(name);
    }

    /**
     * 濡傛灉缁欏畾鐨刡ean鍚嶅瓧鍦╞ean瀹氫箟涓湁鍒悕锛屽垯杩斿洖杩欎簺鍒悕
     *
     * @param name
     * @return
     * @throws NoSuchBeanDefinitionException
     *
     */
    public static String[] getAliases(String name) throws NoSuchBeanDefinitionException
    {
        return getApplicationContext().getAliases(name);
    }

    /**
     * 鑾峰彇aop浠ｇ悊瀵硅薄
     *
     * @param invoker
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAopProxy(T invoker)
    {
        return (T) AopContext.currentProxy();
    }

    /**
     * 鑾峰彇褰撳墠鐨勭幆澧冮厤缃紝鏃犻厤缃繑鍥瀗ull
     *
     * @return 褰撳墠鐨勭幆澧冮厤缃?     */
    public static String[] getActiveProfiles()
    {
        return applicationContext.getEnvironment().getActiveProfiles();
    }

    /**
     * 鑾峰彇褰撳墠鐨勭幆澧冮厤缃紝褰撴湁澶氫釜鐜閰嶇疆鏃讹紝鍙幏鍙栫涓€涓?     *
     * @return 褰撳墠鐨勭幆澧冮厤缃?     */
    public static String getActiveProfile()
    {
        final String[] activeProfiles = getActiveProfiles();
        return activeProfiles.length > 0 ? activeProfiles[0] : null;
    }
}
