package com.platform.agent.multi.memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultMemoryStoreTest {

    @Test
    void testAppendAndRecent() {
        DefaultMemoryStore m = new DefaultMemoryStore();
        m.append("s1", "user", "hi");
        m.append("s1", "assistant", "hello");
        var recent = m.recent("s1", 10);
        assertEquals(2, recent.size());
        assertEquals("user", recent.get(0).get("role"));
        assertEquals("hi", recent.get(0).get("content"));
    }

    @Test
    void testRecentLimit() {
        DefaultMemoryStore m = new DefaultMemoryStore();
        for (int i = 0; i < 10; i++) m.append("s1", "user", "msg-" + i);
        var recent = m.recent("s1", 3);
        assertEquals(3, recent.size());
        assertEquals("msg-7", recent.get(0).get("content"));
        assertEquals("msg-9", recent.get(2).get("content"));
    }

    @Test
    void testMaxCapacity() {
        DefaultMemoryStore m = new DefaultMemoryStore();
        for (int i = 0; i < 200; i++) m.append("s1", "user", "msg-" + i);
        // capacity 100, 前 100 条应被淘汰
        var all = m.recent("s1", 200);
        assertEquals(100, all.size());
        assertEquals("msg-100", all.get(0).get("content"));
    }

    @Test
    void testProfile() {
        DefaultMemoryStore m = new DefaultMemoryStore();
        m.updateProfile("u1", "language", "zh");
        m.updateProfile("u1", "tone", "formal");
        var p = m.getProfile("u1");
        assertEquals("zh", p.get("language"));
        assertEquals("formal", p.get("tone"));
    }

    @Test
    void testClear() {
        DefaultMemoryStore m = new DefaultMemoryStore();
        m.append("s1", "user", "hi");
        m.clear("s1");
        assertTrue(m.recent("s1", 10).isEmpty());
    }

    @Test
    void testNullSession() {
        DefaultMemoryStore m = new DefaultMemoryStore();
        m.append(null, "u", "c");  // 不抛
        assertTrue(m.recent(null, 10).isEmpty());
    }
}
