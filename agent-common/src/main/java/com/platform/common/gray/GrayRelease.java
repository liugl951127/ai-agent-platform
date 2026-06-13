package com.platform.common.gray;

import java.lang.annotation.*;

/**
 * 灰度发布注解
 * <p>
 * 命中灰度规则的请求走"新代码路径",未命中走"老代码路径"。
 * <p>
 * 用法:
 * <pre>
 *   &#64;GrayRelease(resource = "/agent/chat")
 *   public String chat(...) {
 *       if (GrayContext.isGray()) {
 *           return newPath(...);  // 走新逻辑
 *       }
 *       return oldPath(...);      // 走老逻辑
 *   }
 * </pre>
 *
 * 规则匹配策略(由 GrayRuleMatcher 执行):
 *   - USER_ID:  按当前用户 ID 匹配
 *   - TENANT_ID: 按当前租户 ID 匹配
 *   - IP:       按客户端 IP 匹配
 *   - RATIO:    按 userId % 100 落在 [0, ratio) 命中
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrayRelease {
    /** 资源标识,默认取方法全限定名 */
    String resource() default "";
}
