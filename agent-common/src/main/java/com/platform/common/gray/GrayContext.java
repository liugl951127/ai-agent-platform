package com.platform.common.gray;

/**
 * 灰度上下文(基于 ThreadLocal)
 * <p>
 * 由 GrayReleaseAspect 在方法入口设置,业务代码通过 GrayContext.isGray() 判断是否走新路径
 */
public final class GrayContext {
    private GrayContext() {}

    private static final ThreadLocal<Boolean> GRAY = new ThreadLocal<>();

    public static void mark()        { GRAY.set(true); }
    public static void unmark()      { GRAY.set(false); }
    public static void clear()       { GRAY.remove(); }
    public static boolean isGray()   { return Boolean.TRUE.equals(GRAY.get()); }
}
