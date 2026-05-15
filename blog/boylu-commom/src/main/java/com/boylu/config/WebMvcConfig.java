package com.boylu.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boylu.config.interceptor.ApiAccessLogInterceptor;
import com.boylu.entity.SysFileOss;
import com.boylu.enums.FileOssEnum;
import com.boylu.mapper.SysFileOssMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.http.CacheControl;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author: boylu
 * @date 2022/3/10
 * @apiNote
 */
@Configuration
@EnableWebMvc
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SysFileOssMapper sysFileOssMapper;

    @Value("${CORS_ALLOWED_ORIGINS:https://boylu.cn,https://www.boylu.cn,http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080}")
    private String corsAllowedOrigins;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        SysFileOss sysFileOss = sysFileOssMapper.selectOne(new LambdaQueryWrapper<SysFileOss>()
                .eq(SysFileOss::getPlatform, FileOssEnum.LOCAL.getValue()));

        if (sysFileOss != null) {
            registry.addResourceHandler(sysFileOss.getPathPatterns())
                    .addResourceLocations("file:" + sysFileOss.getStoragePath())
                    .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiAccessLogInterceptor());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(Arrays.stream(corsAllowedOrigins.split(","))
                        .map(String::trim)
                        .filter(item -> !item.isEmpty())
                        .toArray(String[]::new))
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With", "Origin", "Accept")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .maxAge(3600);
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
