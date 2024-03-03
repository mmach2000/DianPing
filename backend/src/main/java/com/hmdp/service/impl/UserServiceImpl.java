package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

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

    @Override
    public Result sendCode(String phone, HttpSession session) {

        // 发送短信验证码并保存验证码
        // 1. 判断手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式不正确");
        }
        // 2. 发送验证码
        String code = RandomUtil.randomNumbers(6);
        log.debug("发送验证码：" + code + "到手机号：" + phone);
        // 3. 保存验证码
        session.setAttribute("code", code);
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
        String cachedCode = (String) session.getAttribute("code");
        if (cachedCode == null || !cachedCode.equals(code)) {
            return Result.fail("验证码错误");
        }

        // 3. 验证通过，返回用户信息
        User user = query().eq("phone", phone).one();

        if (user == null) {
            // 用户不存在，自动注册
            user = createUserWithPhone(phone);
        }

        // 4. 保存用户信息到session
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        session.setAttribute("user", userDTO);

        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User().setPhone(phone).setNickName("用户_" + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
