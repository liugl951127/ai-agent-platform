package com.platform.auth.controller;

import com.platform.auth.entity.SysUser;
import com.platform.auth.service.AuthService;
import com.platform.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "鉴权管理", description = "登录/注册/验证码")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "获取图形验证码", description = "返回 key 与验证码(演示用,生产请用 Hutool Captcha)")
    @GetMapping("/captcha")
    public R<String> captcha() { return authService.captcha(); }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<String> login(
            @Parameter(description = "用户名", example = "admin") @RequestParam String username,
            @Parameter(description = "密码", example = "123456") @RequestParam String password) {
        return authService.login(username, password);
    }

    @Operation(summary = "注册新用户")
    @PostMapping("/register")
    public R<String> register(@RequestBody SysUser user) {
        return authService.register(user);
    }
}
