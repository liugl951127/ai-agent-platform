package com.platform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.platform.auth.entity.SysUser;
import com.platform.auth.mapper.SysUserMapper;
import com.platform.common.core.R;
import com.platform.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public R<String> captcha() {
        String code = String.valueOf((int)((Math.random()*9+1)*1000));
        String key = "captcha:" + UUID.randomUUID();
        redis.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        return R.ok(key + "|" + code);
    }

    public R<String> login(String username, String password) {
        SysUser user = userMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", username));
        if (user == null) return R.fail("用户不存在");
        if (!encoder.matches(password, user.getPassword())) return R.fail("密码错误");
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        redis.opsForValue().set("login:" + user.getId(), token, 24, TimeUnit.HOURS);
        return R.ok(token);
    }

    public R<String> register(SysUser user) {
        SysUser exist = userMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", user.getUsername()));
        if (exist != null) return R.fail("用户名已存在");
        user.setPassword(encoder.encode(user.getPassword()));
        userMapper.insert(user);
        return R.ok("注册成功");
    }
}
