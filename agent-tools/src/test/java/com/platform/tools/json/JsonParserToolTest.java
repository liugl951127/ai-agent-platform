package com.platform.tools.json;

import cn.hutool.json.JSONArray;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonParserToolTest {

    private final JsonParserTool tool = new JsonParserTool();

    private final String SAMPLE = "{\"data\":{\"user\":{\"name\":\"alice\",\"age\":30}},\"list\":[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"},{\"id\":3,\"name\":\"alice\"}]}";

    @Test
    void testQuerySimple() {
        Object r = tool.execute(java.util.Map.of("op", "query", "json", SAMPLE, "path", "data.user.name"));
        assertEquals("alice", r);
    }

    @Test
    void testQueryNumber() {
        Object r = tool.execute(java.util.Map.of("op", "query", "json", SAMPLE, "path", "data.user.age"));
        assertEquals(30, ((Number) r).intValue());
    }

    @Test
    void testQueryArray() {
        Object r = tool.execute(java.util.Map.of("op", "query", "json", SAMPLE, "path", "list.1.name"));
        assertEquals("b", r);
    }

    @Test
    void testQueryMissing() {
        Object r = tool.execute(java.util.Map.of("op", "query", "json", SAMPLE, "path", "data.missing.deep"));
        assertNull(r);
    }

    @Test
    void testCountObject() {
        Object r = tool.execute(java.util.Map.of("op", "count", "json", "{\"a\":1,\"b\":2,\"c\":3}"));
        assertEquals(3, r);
    }

    @Test
    void testCountArray() {
        Object r = tool.execute(java.util.Map.of("op", "count", "json", "[1,2,3,4,5]"));
        assertEquals(5, r);
    }

    @Test
    void testKeys() {
        Object r = tool.execute(java.util.Map.of("op", "keys", "json", "{\"a\":1,\"b\":2,\"c\":3}"));
        assertInstanceOf(java.util.Set.class, r);
        assertEquals(3, ((java.util.Set<?>) r).size());
    }

    @Test
    void testFilterEq() {
        Object r = tool.execute(java.util.Map.of(
            "op", "filter", "json", "[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"alice\"},{\"id\":3,\"name\":\"alice\"}]",
            "whereField", "name", "whereOp", "eq", "whereValue", "alice"));
        assertInstanceOf(JSONArray.class, r);
        JSONArray arr = (JSONArray) r;
        assertEquals(2, arr.size());
    }

    @Test
    void testFilterContains() {
        Object r = tool.execute(java.util.Map.of(
            "op", "filter", "json", "[{\"id\":1,\"name\":\"alice\"},{\"id\":2,\"name\":\"bob\"},{\"id\":3,\"name\":\"alicia\"}]",
            "whereField", "name", "whereOp", "contains", "whereValue", "ali"));
        JSONArray arr = (JSONArray) r;
        assertEquals(2, arr.size());
    }

    @Test
    void testFilterGt() {
        Object r = tool.execute(java.util.Map.of(
            "op", "filter", "json", "[{\"id\":1},{\"id\":2},{\"id\":5},{\"id\":3}]",
            "whereField", "id", "whereOp", "gt", "whereValue", "1"));
        JSONArray arr = (JSONArray) r;
        assertEquals(3, arr.size());
    }
}
