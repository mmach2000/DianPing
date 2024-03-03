package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_TOKEN_PREFIX;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取 Token
        String token = request.getHeader("authorization");
        if (token == null) {
            return true;
        }
        // 2. 获取用户信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_TOKEN_PREFIX + token);
        if (userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), true);
        // 3. 将用户信息放入ThreadLocal
        UserHolder.saveUser(userDTO);
        // 4. 刷新 Token 过期时间
        stringRedisTemplate.expire(RedisConstants.LOGIN_TOKEN_PREFIX + token, RedisConstants.LOGIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        // 5. 放行
        return true;
    }
}
