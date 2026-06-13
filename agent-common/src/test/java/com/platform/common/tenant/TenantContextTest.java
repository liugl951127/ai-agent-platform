package com.platform.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantContext 单元测试")
class TenantContextTest {

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    @DisplayName("set/get/clear ThreadLocal 流转")
    void flow() {
        assertNull(TenantContext.getTenantId());
        TenantContext.setTenantId(100L);
        TenantContext.setTenantCode("acme");
        assertEquals(100L, TenantContext.getTenantId());
        assertEquals("acme", TenantContext.getTenantCode());
        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
        assertNull(TenantContext.getTenantCode());
    }
}
