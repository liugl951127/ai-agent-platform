package com.platform.common.gray;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GrayContext 单元测试")
class GrayContextTest {

    @AfterEach
    void tearDown() { GrayContext.clear(); }

    @Test
    @DisplayName("默认未命中")
    void defaultOff() {
        assertFalse(GrayContext.isGray());
    }

    @Test
    @DisplayName("mark 后命中,clear 后未命中")
    void markAndClear() {
        GrayContext.mark();
        assertTrue(GrayContext.isGray());
        GrayContext.clear();
        assertFalse(GrayContext.isGray());
    }
}
