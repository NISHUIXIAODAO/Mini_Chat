package com.easychat.config;

import com.easychat.interceptor.Interceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Resource
    private Interceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(interceptor).addPathPatterns("/**")
                //设置白名单
                .excludePathPatterns("/userInfo/login",//登录
                        "/userInfo/register",
                        "/error",
                        "/userInfo/sendCode",
                        "/*.html",
                        "/*.ico",
                        "/*.html",
                        "/*.css",
                        "/*.js",
                        "/*.png",
                        "/*.jpg",
                        "/webjars/js/**",
                        "/swagger-resources",
                        "/webjars/css/**");
    }
}
