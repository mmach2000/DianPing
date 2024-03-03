package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取Session
        HttpSession session = request.getSession();
        // 2. 获取用户信息
        UserDTO userDTO = (UserDTO) session.getAttribute("user");
        if (userDTO == null) {
            response.setStatus(401);
            return false;
        }
        // 3. 将用户信息放入ThreadLocal
        UserHolder.saveUser(userDTO);
        // 4. 放行
        return true;
    }
}
