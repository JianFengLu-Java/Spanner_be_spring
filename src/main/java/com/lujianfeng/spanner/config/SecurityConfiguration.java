package com.lujianfeng.spanner.config;

import com.lujianfeng.spanner.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF 保护 (REST API 通常不需要会话，因此禁用)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 禁用默认表单登录
                .formLogin(AbstractHttpConfigurer::disable)

                // 3. 禁用 HTTP Basic 认证
                .httpBasic(AbstractHttpConfigurer::disable)

                // 4. 配置授权规则 (根据您的需求配置，例如允许所有请求)
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/user/login","/user/register").permitAll()
                                // 允许所有请求访问，但仍然需要认证 (如果配置了认证机制)
                                .anyRequest().authenticated()
                        // 或者，如果需要认证：
                        // .anyRequest().authenticated()
                );
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
