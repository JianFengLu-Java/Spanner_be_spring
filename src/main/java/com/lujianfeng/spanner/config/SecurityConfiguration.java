package com.lujianfeng.spanner.config;

import com.lujianfeng.spanner.filter.CustomFilter;
import com.lujianfeng.spanner.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


/**
 * Security configuration class
 *
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@Configuration
@EnableMethodSecurity(prePostEnabled = true)

public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomFilter customFilter;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter, CustomFilter customFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customFilter = customFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 1. 禁用 CSRF 保护 (REST API 通常不需要会话，因此禁用)不需要Session
                .csrf(AbstractHttpConfigurer::disable)
                // 2. 禁用默认表单登录，如果访问受保护的接口时，跳出来登录页面，在这里可以关闭
                .formLogin(AbstractHttpConfigurer::disable)
                // 3. 禁用 HTTP Basic 认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 4. 配置授权规则 (根据需求配置)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/login",
                                "/user/register",
                                "/ws/**",
                                "/files/update/avatar/**",
                                "/.~~spring-boot!~/remote-update")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
        ;

        http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("http://localhost:5173");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
