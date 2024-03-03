package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {

        // 发送短信验证码并保存验证码
        // 1. 判断手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式不正确");
        }
        // 2. 发送验证码
        String code = RandomUtil.randomNumbers(6);
        log.debug("发送验证码：" + code + " 到手机号：" + phone);
        // 3. 保存验证码到 Redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_PREFIX + phone, code, RedisConstants.LOGIN_CODE_TTL_MINUTES, TimeUnit.MINUTES);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();

        // 1. 判断手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式不正确");
        }

        // 2. 判断验证码是否合法
        if (RegexUtils.isCodeInvalid(code)) {
            return Result.fail("验证码格式不正确");
        }
        String cachedCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_PREFIX + phone);
        if (cachedCode == null || !cachedCode.equals(code)) {
            return Result.fail("验证码错误");
        }

        // 3. 验证通过，返回用户信息
        User user = query().eq("phone", phone).one();

        if (user == null) {
            // 用户不存在，自动注册
            user = createUserWithPhone(phone);
        }

        // 4. 保存用户信息到 Redis
        // 4.1 生成登录 Token
        String token = UUID.fastUUID().toString();
        // 4.2 保存用户信息到 Redis
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((name, value) -> value instanceof String ? value : value.toString()));
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_TOKEN_PREFIX + token, userMap);
        stringRedisTemplate.expire(RedisConstants.LOGIN_TOKEN_PREFIX + token, RedisConstants.LOGIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User().setPhone(phone).setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
