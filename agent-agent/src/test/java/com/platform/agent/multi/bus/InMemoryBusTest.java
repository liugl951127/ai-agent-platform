package com.platform.agent.multi.bus;

import com.platform.agent.multi.AgentMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBusTest {

    @Test
    void testPointToPoint() {
        InMemoryBus bus = new InMemoryBus();
        bus.publish(AgentMessage.builder()
            .from("a").to("b").topic("hi").body("hello").timestamp(0).build());
        List<AgentMessage> got = bus.poll("b", 10);
        assertEquals(1, got.size());
        assertEquals("hi", got.get(0).getTopic());
    }

    @Test
    void testSubscribe() {
        InMemoryBus bus = new InMemoryBus();
        AtomicInteger n = new AtomicInteger(0);
        bus.subscribe("a", m -> n.incrementAndGet());
        bus.publish(AgentMessage.builder().from("x").to("a").topic("t").body("b").build());
        bus.publish(AgentMessage.builder().from("x").to("a").topic("t").body("b").build());
        assertEquals(2, n.get());
    }

    @Test
    void testBroadcast() {
        InMemoryBus bus = new InMemoryBus();
        // 先有订阅者, broadcast 才到
        bus.subscribe("a", m -> {});
        bus.subscribe("b", m -> {});
        bus.subscribe("c", m -> {});

        bus.publish(AgentMessage.builder().from("a").to("all").topic("broadcast").body("!").build());

        assertEquals(1, bus.poll("b", 10).size());
        assertEquals(1, bus.poll("c", 10).size());
        // a 不会收到自己的广播
        assertEquals(0, bus.poll("a", 10).size());
    }

    @Test
    void testPollLimit() {
        InMemoryBus bus = new InMemoryBus();
        for (int i = 0; i < 5; i++) {
            bus.publish(AgentMessage.builder().from("x").to("y").topic("t" + i).body("").build());
        }
        List<AgentMessage> got = bus.poll("y", 3);
        assertEquals(3, got.size());
        assertEquals("t0", got.get(0).getTopic());
        // 再拉
        List<AgentMessage> got2 = bus.poll("y", 10);
        assertEquals(2, got2.size());
    }

    @Test
    void testUnsubscribe() {
        InMemoryBus bus = new InMemoryBus();
        List<AgentMessage> seen = new ArrayList<>();
        java.util.function.Consumer<AgentMessage> handler = seen::add;
        bus.subscribe("a", handler);
        bus.publish(AgentMessage.builder().from("x").to("a").topic("t").body("").build());
        bus.unsubscribe("a", handler);
        bus.publish(AgentMessage.builder().from("x").to("a").topic("t").body("").build());
        assertEquals(1, seen.size());
    }

    @Test
    void testPollEmpty() {
        InMemoryBus bus = new InMemoryBus();
        assertTrue(bus.poll("ghost", 10).isEmpty());
    }
}
