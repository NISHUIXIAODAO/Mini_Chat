package com.easychat.interceptor;

import com.alibaba.fastjson.JSON;
import com.easychat.entity.ResultVo;
import com.easychat.service.IJWTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/***
 * 拦截方法类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Interceptor implements HandlerInterceptor {
    private final IJWTService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // CORS 预检请求不携带业务 token，直接放行给 Spring MVC 处理。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = jwtService.extractToken(request);
        if (token == null) {
            log.warn("用户未登录，URI: {}", request.getRequestURI());
            writeUnauthorized(response, "请先登录");
            return false;
        }

        if (!jwtService.verifyToken(token)) {
            log.warn("Token 校验失败，URI: {}", request.getRequestURI());
            writeUnauthorized(response, "登录状态已失效，请重新登录");
            return false;
        }
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        // 拦截器返回 false 时不会进入 Controller，这里直接写出统一的 JSON 错误体。
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSON.toJSONString(ResultVo.unauthorized(message)));
    }
}
