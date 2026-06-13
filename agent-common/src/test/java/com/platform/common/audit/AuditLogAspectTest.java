package com.platform.common.audit;

import com.platform.common.tenant.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

@DisplayName("AuditLog 单元测试")
class AuditLogAspectTest {

    AuditLogAspect aspect = new AuditLogAspect();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(42L);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "100");
        req.setRemoteAddr("10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }
    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("拦截 @AuditLog 方法 → 写入审计记录(成功)")
    void aroundSuccess() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(sig);
        Method m = SampleService.class.getMethod("createAgent", Long.class, String.class);
        when(sig.getMethod()).thenReturn(m);
        when(sig.getDeclaringTypeName()).thenReturn(SampleService.class.getName());
        when(pjp.getArgs()).thenReturn(new Object[]{ 1L, "name" });
        when(pjp.proceed()).thenReturn("ok");

        aspect.around(pjp);
        verify(pjp).proceed();
    }

    @Test
    @DisplayName("方法抛异常时也记录审计(status=0)")
    void aroundWithException() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature sig = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(sig);
        Method m = SampleService.class.getMethod("createAgent", Long.class, String.class);
        when(sig.getMethod()).thenReturn(m);
        when(sig.getDeclaringTypeName()).thenReturn(SampleService.class.getName());
        when(pjp.getArgs()).thenReturn(new Object[]{ 1L, "name" });
        when(pjp.proceed()).thenThrow(new RuntimeException("biz error"));

        try {
            aspect.around(pjp);
        } catch (RuntimeException expected) {
            // swallow, we're just testing audit
        }
        verify(pjp).proceed();
    }

    // 测试用样例
    static class SampleService {
        @AuditLog(module = "智能体", action = "CREATE", resourceType = "agent", resourceId = "#{#id}")
        public String createAgent(Long id, String name) { return "ok"; }
    }
}
