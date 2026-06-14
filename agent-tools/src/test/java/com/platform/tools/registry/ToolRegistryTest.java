package com.platform.tools.registry;

import com.platform.tools.calculator.CalculatorTool;
import com.platform.tools.datetime.DateTimeTool;
import com.platform.tools.json.JsonParserTool;
import com.platform.tools.api.Tool;
import com.platform.tools.api.ToolDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolRegistry 自动发现测试 (用裸 ApplicationContext 模拟 Spring 启动)
 */
class ToolRegistryTest {

    private ToolRegistry bootWith(Tool... extra) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(ToolRegistry.class);
        // 注册测试工具
        ctx.registerBean(CalculatorTool.class);
        ctx.registerBean(DateTimeTool.class);
        ctx.registerBean(JsonParserTool.class);
        for (Tool t : extra) ctx.registerBean((Class<Tool>) t.getClass());
        ctx.refresh();
        ToolRegistry reg = ctx.getBean(ToolRegistry.class);
        reg.init();
        return reg;
    }

    @Test
    void testAutoDiscovery() {
        ToolRegistry reg = bootWith();
        assertTrue(reg.names().size() >= 3);
        assertTrue(reg.names().contains("calculator"));
        assertTrue(reg.names().contains("datetime"));
        assertTrue(reg.names().contains("json"));
    }

    @Test
    void testInvokeCalculator() throws Exception {
        ToolRegistry reg = bootWith();
        Object r = reg.invoke("calculator", Map.of("expression", "2*(3+4)"));
        assertEquals(14.0, ((Number) r).doubleValue(), 1e-9);
    }

    @Test
    void testInvokeJson() throws Exception {
        ToolRegistry reg = bootWith();
        Object r = reg.invoke("json", Map.of("op", "count", "json", "[1,2,3,4,5]"));
        assertEquals(5, r);
    }

    @Test
    void testInvokeUnknown() {
        ToolRegistry reg = bootWith();
        assertThrows(IllegalArgumentException.class,
            () -> reg.invoke("nonexistent", Map.of()));
    }

    @Test
    void testToLlmFunctions() {
        ToolRegistry reg = bootWith();
        var fns = reg.toLlmFunctions();
        assertFalse(fns.isEmpty());
        var first = fns.get(0);
        assertEquals("function", first.get("type"));
        assertTrue(first.containsKey("function"));
        @SuppressWarnings("unchecked")
        Map<String, Object> fn = (Map<String, Object>) first.get("function");
        assertTrue(fn.containsKey("name"));
        assertTrue(fn.containsKey("description"));
        assertTrue(fn.containsKey("parameters"));
    }

    @Test
    void testToPromptText() {
        ToolRegistry reg = bootWith();
        String text = reg.toPromptText();
        assertTrue(text.contains("calculator"));
        assertTrue(text.contains("datetime"));
        assertTrue(text.contains("json"));
    }

    @Test
    void testCategory() {
        ToolRegistry reg = bootWith();
        var mathTools = reg.byCategory("math");
        assertTrue(mathTools.contains("calculator"));
    }
}
