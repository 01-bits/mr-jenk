package com.buy01.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import jakarta.servlet.http.HttpServletRequest;

public class FeignSupportConfig {

    @Value("${AUTH_USER_ID_HEADER}")
    private String AUTH_USER_ID_HEADER;

    @Value("${AUTH_USER_ROLE_HEADER}")
    private String AUTH_USER_ROLE_HEADER;

    @Bean
    public RequestInterceptor requestTokenInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userId = request.getHeader(AUTH_USER_ID_HEADER);
                if (userId != null) {
                    requestTemplate.header(AUTH_USER_ID_HEADER, userId);
                }
                String role = request.getHeader(AUTH_USER_ROLE_HEADER);
                if (role != null) {
                    requestTemplate.header(AUTH_USER_ROLE_HEADER, role);
                }
            }
        };
    }

    @Bean
    public Encoder multipartFormEncoder() {
        return new SpringFormEncoder();
    }
}