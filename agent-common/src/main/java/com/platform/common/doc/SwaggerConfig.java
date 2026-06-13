package com.platform.common.doc;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一 Swagger / OpenAPI 3 配置
 * <p>
 * 各微服务只需在 application.yml 配 springdoc.api-docs.enabled=true,
 * 即可在 ${server.servlet.context-path:-}/doc.html 看到文档。
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name:unknown-service}")
    private String appName;

    @Bean
    public OpenAPI platformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Agent Platform - " + appName)
                        .description("分布式大模型智能体平台 - 微服务 " + appName + " 接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("liugl951127")
                                .url("https://github.com/liugl951127/ai-agent-platform")
                                .email("agent@platform.local"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
