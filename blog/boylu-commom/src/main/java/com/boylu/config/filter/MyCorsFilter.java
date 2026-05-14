package com.boylu.config.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple CORS filter.
 *
 * @author: boylu
 * @date 2022/3/11
 */
@Component
public class MyCorsFilter implements Filter {

    @Value("${CORS_ALLOWED_ORIGINS:https://boylu.cn,https://www.boylu.cn,http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080}")
    private String corsAllowedOrigins;

    private Set<String> getAllowedOrigins() {
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isAllowedOrigin(String origin) {
        return origin != null && getAllowedOrigins().contains(origin);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String origin = httpRequest.getHeader("Origin");
        if (isAllowedOrigin(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Vary", "Origin");
        }
        httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,X-Requested-With,Origin,Accept");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(200);
            return;
        }
        filterChain.doFilter(servletRequest, httpResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
