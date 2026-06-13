package com.platform.common.security;

public class SecurityContext {
    private static final ThreadLocal<Long> USER = new ThreadLocal<>();
    public static void setUserId(Long id) { USER.set(id); }
    public static Long getUserId() { return USER.get(); }
    public static void clear() { USER.remove(); }
}
