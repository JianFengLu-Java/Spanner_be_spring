package com.lujianfeng.spanner.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/10
 * @context <p>
 * SpringBoot中的Security过滤器学习
 * <p>
 * 自定义的过滤器
 * <p>
 * 首先要继承Spring Security 中的 OncePreRequestFilter
 * 并实现这个抽象父类中的抽象方法 doFilterInternal
 * @since 1.0
 */
@Component

public class CustomFilter extends OncePerRequestFilter {


    private final Logger logger = LoggerFactory.getLogger(CustomFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        logger.info("自定义过滤器执行");
        filterChain.doFilter(request, response);
    }
}
