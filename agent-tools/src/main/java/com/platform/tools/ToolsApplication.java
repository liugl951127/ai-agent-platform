package com.platform.tools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 工具能力中心 (独立服务)
 * <p>
 * 暴露 HTTP 接口:
 *   GET  /tools/list                  列出所有工具 + JSON Schema
 *   GET  /tools/list?format=prompt    文本格式 (ReAct prompt 友好)
 *   POST /tools/invoke                远程调用工具 {name, args}
 *   POST /tools/invoke/batch          批量调用 [{name, args}, ...]
 * <p>
 * 其他服务 (agent-agent / agent-workflow) 通过 HTTP 或类路径共享使用
 */
@SpringBootApplication
@ComponentScan({"com.platform.tools", "com.platform.common"})
public class ToolsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ToolsApplication.class, args);
    }
}
