package com.easychat.interceptor;

import com.easychat.service.impl.JWTServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/***
 * 拦截方法类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Interceptor implements HandlerInterceptor {
//    @Autowired
    private final JWTServiceImpl jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //判断token是否有效
        System.out.println("拦截成功");
        //我们先拿到访问接口
        StringBuffer requestURL = request.getRequestURL();
        System.out.println("访问接口：" + requestURL);
        //拿到token
        String token = request.getHeader("authorization");
        log.info("拿到的token为：{}",token);
        if(token == null){
            log.error("用户未登录");
            return false;
        }
        //检查token有效性
        //解析token
        //查询用户是否存在数据库中
        return jwtService.verifyToken(token);
    }
}
