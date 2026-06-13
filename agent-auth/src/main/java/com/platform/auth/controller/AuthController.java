package com.platform.auth.controller;

import com.platform.auth.entity.SysUser;
import com.platform.auth.service.AuthService;
import com.platform.common.core.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/captcha")
    public R<String> captcha() { return authService.captcha(); }

    @PostMapping("/login")
    public R<String> login(@RequestParam String username,
                           @RequestParam String password) {
        return authService.login(username, password);
    }

    @PostMapping("/register")
    public R<String> register(@RequestBody SysUser user) {
        return authService.register(user);
    }
}
