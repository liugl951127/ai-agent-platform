package com.platform.tools.http;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SSRFGuardTest {

    @Test
    void testAllowHttps() {
        assertDoesNotThrow(() -> SSRFGuard.check("https://www.baidu.com"));
        assertDoesNotThrow(() -> SSRFGuard.check("http://example.com/path?q=1"));
    }

    @Test
    void testRejectLocalhost() {
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check("http://localhost/admin"));
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check("http://127.0.0.1/x"));
    }

    @Test
    void testRejectPrivateIp() {
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check("http://10.0.0.1/x"));
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check("http://192.168.1.1/x"));
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check("http://172.16.0.1/x"));
    }

    @Test
    void testRejectLinkLocal() {
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check("http://169.254.169.254/latest/meta-data/"));
    }

    @Test
    void testRejectFileScheme() {
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check("gopher://x"));
    }

    @Test
    void testRejectNoScheme() {
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check("www.baidu.com"));
    }

    @Test
    void testRejectNull() {
        assertThrows(IllegalArgumentException.class, () -> SSRFGuard.check(null));
    }
}
