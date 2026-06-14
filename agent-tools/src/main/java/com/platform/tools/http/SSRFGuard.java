package com.platform.tools.http;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * SSRF 防护 — 拒绝访问 localhost / 私网 IP / link-local
 * <p>
 * Agent 调用 http_fetch 时强校验, 防止 LLM 被 prompt injection 后访问内网
 */
public final class SSRFGuard {

    private SSRFGuard() {}

    private static final Set<String> BLOCKED_SCHEMES = Set.of("file", "gopher", "ldap", "jar");

    public static void check(String urlStr) {
        if (urlStr == null) throw new IllegalArgumentException("URL 为空");
        URI uri;
        try { uri = URI.create(urlStr); }
        catch (Exception e) { throw new IllegalArgumentException("URL 解析失败: " + urlStr, e); }
        String scheme = uri.getScheme();
        if (scheme == null) throw new IllegalArgumentException("URL 缺 scheme: " + urlStr);
        scheme = scheme.toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("不允许的 scheme: " + scheme);
        }
        if (BLOCKED_SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException("禁止 scheme: " + scheme);
        }
        String host = uri.getHost();
        if (host == null) throw new IllegalArgumentException("URL 缺 host");
        // host 可能是 IP 字面量
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            for (InetAddress addr : addrs) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() ||
                    addr.isLinkLocalAddress() || addr.isAnyLocalAddress() ||
                    isPrivate(addr)) {
                    throw new IllegalArgumentException("禁止访问私网 / loopback 地址: " + host + " -> " + addr.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            // DNS 解析失败也放行 (hutool 自己会报错)
        }
    }

    private static boolean isPrivate(InetAddress addr) {
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
            int o0 = b[0] & 0xFF;
            int o1 = b[1] & 0xFF;
            if (o0 == 10) return true;
            if (o0 == 172 && o1 >= 16 && o1 <= 31) return true;
            if (o0 == 192 && o1 == 168) return true;
            // 169.254.0.0/16
            if (o0 == 169 && o1 == 254) return true;
        }
        return false;
    }
}
