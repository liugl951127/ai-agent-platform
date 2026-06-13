package com.platform.common.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("R<T> 统一返回")
class RTest {

    @Test
    @DisplayName("ok() 默认 code=200 message=success")
    void okDefault() {
        R<String> r = R.ok();
        assertEquals(200, r.getCode());
        assertEquals("success", r.getMessage());
        assertNull(r.getData());
        assertNotNull(r.getTimestamp());
    }

    @Test
    @DisplayName("ok(data) 携带数据")
    void okWithData() {
        R<Integer> r = R.ok(42);
        assertEquals(200, r.getCode());
        assertEquals(42, r.getData());
    }

    @Test
    @DisplayName("fail(msg) 默认 code=500")
    void failDefault() {
        R<?> r = R.fail("出错了");
        assertEquals(500, r.getCode());
        assertEquals("出错了", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    @DisplayName("fail(code, msg) 自定义 code")
    void failWithCode() {
        R<?> r = R.fail(429, "限流");
        assertEquals(429, r.getCode());
        assertEquals("限流", r.getMessage());
    }
}
