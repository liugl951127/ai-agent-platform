package com.platform.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil 单元测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-must-be-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtUtil, "expire", 60_000L);
    }

    @Test
    @DisplayName("生成 token 后能正确解析出 uid / username")
    void generateAndParse() {
        String token = jwtUtil.generate(42L, "alice");
        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = jwtUtil.parse(token);
        assertEquals("alice", claims.getSubject());
        assertEquals(42, ((Number) claims.get("uid")).longValue());
    }

    @Test
    @DisplayName("过期 token 解析抛异常")
    void expiredTokenThrows() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtil, "expire", 1L);
        String token = jwtUtil.generate(1L, "bob");
        Thread.sleep(50);
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtUtil.parse(token));
    }

    @Test
    @DisplayName("伪造 token 解析抛异常")
    void tamperedTokenThrows() {
        String token = jwtUtil.generate(7L, "eve");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThrows(io.jsonwebtoken.JwtException.class,
                () -> jwtUtil.parse(tampered));
    }
}
