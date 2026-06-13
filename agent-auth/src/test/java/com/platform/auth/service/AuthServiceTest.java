package com.platform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.platform.auth.entity.SysUser;
import com.platform.auth.mapper.SysUserMapper;
import com.platform.common.core.R;
import com.platform.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @Mock SysUserMapper userMapper;
    @Mock JwtUtil jwtUtil;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks AuthService authService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        // AuthService 自带 BCryptPasswordEncoder,不通过 Spring 注入
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("登录成功:返回 token")
    void loginSuccess() {
        SysUser u = new SysUser();
        u.setId(1L);
        u.setUsername("admin");
        u.setPassword(encoder.encode("123456"));
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(u);
        when(jwtUtil.generate(1L, "admin")).thenReturn("mock-jwt-token");

        R<String> r = authService.login("admin", "123456");
        assertEquals(200, r.getCode());
        assertEquals("mock-jwt-token", r.getData());
        verify(valueOps).set(eq("login:1"), eq("mock-jwt-token"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("登录失败:用户不存在")
    void loginUserNotFound() {
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        R<String> r = authService.login("ghost", "any");
        assertEquals(500, r.getCode());
        assertEquals("用户不存在", r.getMessage());
        verify(jwtUtil, never()).generate(anyLong(), anyString());
    }

    @Test
    @DisplayName("登录失败:密码错误")
    void loginWrongPassword() {
        SysUser u = new SysUser();
        u.setId(2L);
        u.setPassword(encoder.encode("correct"));
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(u);
        R<String> r = authService.login("alice", "wrong");
        assertEquals(500, r.getCode());
        assertEquals("密码错误", r.getMessage());
    }

    @Test
    @DisplayName("注册成功")
    void registerSuccess() {
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        SysUser u = new SysUser();
        u.setUsername("newuser");
        u.setPassword("raw-pass");
        R<String> r = authService.register(u);
        assertEquals(200, r.getCode());
        // 密码应该被 BCrypt
        assertNotEquals("raw-pass", u.getPassword());
        assertTrue(u.getPassword().startsWith("$2"));
        verify(userMapper).insert(u);
    }

    @Test
    @DisplayName("注册失败:用户名重复")
    void registerDuplicate() {
        SysUser exist = new SysUser();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(exist);
        SysUser u = new SysUser();
        u.setUsername("admin");
        R<String> r = authService.register(u);
        assertEquals(500, r.getCode());
        assertEquals("用户名已存在", r.getMessage());
        verify(userMapper, never()).insert(any());
    }
}
