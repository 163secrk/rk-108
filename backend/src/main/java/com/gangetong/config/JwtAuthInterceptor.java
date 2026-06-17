package com.gangetong.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangetong.common.Result;
import com.gangetong.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT登录认证拦截器
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    /**
     * 请求处理前拦截验证Token
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域OPTIONS请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 获取请求头中的Token
        String authHeader = request.getHeader(header);

        if (authHeader == null || !authHeader.startsWith(prefix)) {
            // Token不存在或格式错误
            writeErrorResponse(response, Result.unauthorized("请先登录"));
            return false;
        }

        // 提取Token（去除前缀）
        String token = authHeader.substring(prefix.length());

        try {
            // 验证Token有效性
            if (!jwtUtil.validateToken(token)) {
                writeErrorResponse(response, Result.unauthorized("登录已过期，请重新登录"));
                return false;
            }

            // 将用户信息存入Request属性，供Controller使用
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);

            return true;
        } catch (Exception e) {
            writeErrorResponse(response, Result.unauthorized("Token无效，请重新登录"));
            return false;
        }
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, Result<?> result) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(result));
    }
}
