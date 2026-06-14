package com.platform.tools.datetime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DateTimeToolTest {

    private final DateTimeTool tool = new DateTimeTool();

    @Test
    void testNow() {
        Object r = tool.execute(java.util.Map.of("op", "now"));
        assertNotNull(r);
        assertTrue(r.toString().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testAdd() {
        Object r = tool.execute(java.util.Map.of(
            "op", "add", "base", "2026-01-01 00:00:00",
            "amount", 7, "unit", "DAY"));
        assertEquals("2026-01-08 00:00:00", r);
    }

    @Test
    void testAddNegative() {
        Object r = tool.execute(java.util.Map.of(
            "op", "add", "base", "2026-01-10 00:00:00",
            "amount", -3, "unit", "DAY"));
        assertEquals("2026-01-07 00:00:00", r);
    }

    @Test
    void testDiff() {
        Object r = tool.execute(java.util.Map.of(
            "op", "diff", "base", "2026-01-01 00:00:00",
            "time", "2026-01-11 00:00:00", "unit", "DAY"));
        assertInstanceOf(java.util.Map.class, r);
        java.util.Map<?,?> m = (java.util.Map<?,?>) r;
        assertEquals(10L, ((Number) m.get("amount")).longValue());
    }

    @Test
    void testFormat() {
        Object r = tool.execute(java.util.Map.of(
            "op", "format", "time", "2026-01-01 12:30:45",
            "pattern", "yyyy/MM/dd"));
        assertEquals("2026/01/01", r);
    }

    @Test
    void testZone() {
        Object r = tool.execute(java.util.Map.of(
            "op", "zone", "time", "2026-01-01 10:00:00",
            "fromZone", "UTC", "toZone", "Asia/Shanghai"));
        assertEquals("2026-01-01 18:00:00", r);  // +8h
    }

    @Test
    void testInvalidOp() {
        assertThrows(IllegalArgumentException.class,
            () -> tool.execute(java.util.Map.of("op", "foo")));
    }
}
