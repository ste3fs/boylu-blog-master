package com.boylu.config.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private static final String[] PUBLIC_PATHS = new String[]{
            "/auth/login",
            "/auth/getCaptcha",
            "/auth/verify",
            "/swagger-ui/**",
            "/webjars/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/favicon.ico",
            "/swagger-resources",
            "/api/auth/render/**",
            "/api/auth/callback/**",
            "/api/sendEmailCode",
            "/api/email/register",
            "/api/email/forgot",
            "/api/wechat/getCode",
            "/api/wechat/isLogin/**",
            "/api/wechat/appletLogin/**",
            "/api/webConfig",
            "/api/getNotice",
            "/api/getHotSearch/**",
            "/api/report",
            "/api/article/list",
            "/api/article/home-list",
            "/api/article/detail/**",
            "/api/article/archive",
            "/api/article/categories",
            "/api/article/categorie-all",
            "/api/article/getCarousels",
            "/api/article/getRecommends",
            "/api/album/list",
            "/api/album/detail/**",
            "/api/album/photos/**",
            "/api/album/verify/**",
            "/api/comment/list",
            "/api/friend/list",
            "/api/friend/apply",
            "/api/message/list",
            "/api/message/add",
            "/api/moment/list",
            "/api/tag/list",
            "/api/resource/list",
            "/api/resource/verify",
            "/api/perf/report",
            "/wechat/**",
            "/img/**",
            "/localFile/**",
            "/file/content/**",
            "/file/view/**",
            "/file/by-url",
            "/file/by-url/**",
            "/boylu/file/content/**",
            "/boylu/file/view/**",
            "/boylu/file/by-url",
            "/boylu/file/by-url/**",
            "/mojian/file/content/**",
            "/mojian/file/view/**",
            "/mojian/file/by-url",
            "/mojian/file/by-url/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(PUBLIC_PATHS);
    }
}
