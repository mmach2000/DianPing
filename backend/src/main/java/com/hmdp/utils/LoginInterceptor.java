package com.hmdp.utils;

import jakarta.annotation.Nonnull;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {
        // 判断是否登录
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
