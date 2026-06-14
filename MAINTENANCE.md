# 维护指南(MAINTENANCE)

> 收录依赖版本矩阵、镜像源、最佳实践,方便接手人 / 一年后的自己快速定位。

## 1. 依赖版本矩阵

> 全部版本集中在 **父 pom.xml 的 `<properties>`**,**不要**在子模块里写死。

| 依赖 | 版本 | 来源 | 备注 |
|---|---|---|---|
| Spring Boot | 3.2.5 | `spring-boot-starter-parent` | 父级 |
| Spring Cloud | 2023.0.1 | BOM import | 与 SB 3.2 兼容 |
| Spring Cloud Alibaba | 2023.0.1.0 | BOM import | 与 SC 2023.0.1 绑定 |
| Nacos Client | 由 SCA BOM 管 | `spring-cloud-starter-alibaba-nacos-{discovery,config}` | |
| Sentinel | 1.8.6 | 显式声明 | SCA BOM 不管这几个 artifactId |
| Seata | 2.0.0 | 显式声明 | 与 Spring Boot 3 兼容 |
| Redisson | **3.34.1** | redisson-bom | **覆盖 SB 自带的 3.27.2**(阿里云缺失) |
| MyBatis-Plus | 3.5.5 | 显式声明 | `-spring-boot3-starter` 变体 |
| Flowable | 7.0.1 | 显式声明 | Spring Boot 3 兼容 |
| Hutool | 5.8.27 | 显式声明 | |
| Knife4j | 4.5.0 | 显式声明 | `-openapi3-jakarta-` 前缀 |
| JJWT | 0.12.5 | 显式声明 | jakarta 变体 |
| Swagger 注解 | 2.2.22 | 显式声明 | `-jakarta` 后缀 |
| Micrometer | SB 管 | spring-boot-starter-actuator | |
| MinIO | 8.5.10 | 显式声明 | agent-file 用 |
| Tika | 2.9.2 | 显式声明 | agent-knowledge 用 |
| Spring AI | 1.0.0-M5 | 显式声明 | **Milestone,需额外仓库** |

## 2. Maven 镜像源配置

**推荐 `~/.m2/settings.xml`**(国内网络):

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun-central</id>
      <name>Aliyun Central</name>
      <url>https://maven.aliyun.com/repository/central</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
    <mirror>
      <id>aliyun-public</id>
      <name>Aliyun Public</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>*,!aliyun-central,!spring-milestones</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

注意:
- ❌ 不要把 `spring-milestones` 也 mirror,会拉不到 SB AI 的 Milestone 包
- ✅ 阿里云镜像会**自动**同步 Maven Central,所以 `mirrorOf=*` 也能用(只是偶尔有 24h 延迟)

## 3. 常见 mvn 错误速查

| 错误信息 | 原因 | 修复 |
|---|---|---|
| `'dependencies.dependency.version' for xxx is missing` | 子模块写了未在父 BOM 中的依赖 | 在父 POM `<dependencyManagement>` 显式声明 |
| `Could not find artifact org.redisson:redisson-bom:pom:3.27.2` | 阿里云镜像缺这个版本 | 父 POM 显式 import `redisson-bom:3.34.1` |
| `Plugin ... requires Maven version 3.6.3` | 本地 Maven 太老 | 升 Maven 到 3.9.x;或加 `maven-enforcer-plugin` |
| `Failed to read artifact descriptor for xxx:jar` | 本地 .m2 缓存损坏 | `mvn dependency:purge-local-repository -DreResolve=false` |
| `Package ... does not exist` | 子模块依赖没在父 POM 管 | 父 dependencyManagement 加上 |
| `java: error: invalid target release: 17` | JDK < 17 | 装 JDK 17,在 `JAVA_HOME` 配好 |

## 4. 升级依赖流程

### 升级 Spring Boot
1. 改父 POM `<parent>` 的 `<version>`
2. 同步改 `spring-cloud.version` 到对应兼容表(参考 [Spring Cloud 兼容矩阵](https://spring.io/projects/spring-cloud#overview))
3. 同步改 `spring-cloud-alibaba.version`
4. `mvn clean install -U` 验证

### 升级 Redisson
1. 改 `<redisson.version>`
2. 删 `~/.m2/repository/org/redisson` 强制重拉
3. `mvn clean install -U`

### 新增第三方依赖
1. 父 POM `<properties>` 加版本
2. 父 POM `<dependencyManagement>` 加 dependency(不写 scope)
3. 子模块 pom 直接引(不写 version)
4. `mvn clean install -U`

## 5. CI 必跑命令

本地跑通再 push:

```bash
mvn clean install -DskipTests         # 编译
mvn test                              # 跑测试
mvn verify -Pqa                       # 跑 quality 检查(可选)
mvn dependency:tree -Dincludes=org.redisson   # 看依赖树
```

## 6. 数据库迁移规范

- 任何 schema 变更:放 `sql/migration_*.sql` 文件
- 文件名带时间戳:`migration_20240101_xxx.sql`
- 每个文件**只**做一件事(加列 / 加表 / 改索引,不要混合)
- init.sql 保持不变,只用于全新初始化

## 7. Git 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

type 枚举:
- `feat`: 新功能
- `fix`: 修 bug
- `chore`: 构建/CI/工具
- `docs`: 文档
- `refactor`: 重构(既不是 feat 也不是 fix)
- `test`: 测试
- `perf`: 性能优化

示例:
```
feat(llm): 加 Qwen Provider
fix(gateway): 修 JWT 过期返 500 应返 401
docs(readme): 补 Docker 部署步骤
```

## 7. agent-gateway 特殊处理 (WebFlux)

`agent-gateway` 用的是 **Spring Cloud Gateway (WebFlux)**, 跟其他 7 个服务 (servlet) 不一样:

| 注意点 | 处理方式 |
|---|---|
| 启动类不加 `@ComponentScan("com.platform.common")` | common 里的 `TenantInterceptor` / `AuditLogAspect` / `SentinelConfig` 都引用了 `jakarta.servlet.*`,WebFlux 启动时 cglib 增强这些类会 NoClassDefFoundError |
| 显式 `@Import(JwtUtil.class)` 引入需要的 bean | 网关只用到 JwtUtil 校验 token, 不需要其他 servlet-based 拦截器 |
| 鉴权走 WebFlux `GlobalFilter` | 不要写 `OncePerRequestFilter` (servlet) |
| Redis / 分布式锁 | Redisson 有 webflux 适配 (不依赖 servlet), 但目前未在 gateway 启用, 按需引入 |

**踩过的坑**:
- 一开始给 gateway 加 `@ComponentScan("com.platform.common")` 启动失败: `NoClassDefFoundError: jakarta/servlet/http/HttpServletRequest`
- 删除后,AuthFilter 找不到 JwtUtil → 用 `@Import(JwtUtil.class)` 解决


## 8. agent-common 的 @ConditionalOnClass 保护清单

`agent-common` 是共享库,被 8 个服务依赖。但各服务实际用到的子集不一样 (有的用 MyBatis-Plus, 有的用 Redisson, 有的用 Sentinel)。

为了避免**没用到的服务因 class 引用了不存在的类而启动失败**, 以下类加了 `@ConditionalOnClass(name = "FQCN")` 保护:

| 类 | 条件 | 说明 |
|---|---|---|
| `MybatisPlusConfig` | mybatis-plus 在 classpath | 分页插件 |
| `MybatisMetaObjectHandler` | mybatis-plus 在 classpath | 自动填充 createTime / tenantId |
| `RedissonConfig` | redisson 在 classpath | 反射初始化 RedissonClient (避免编译期 import) |
| `RedissonUtil` | redisson-api 在 classpath | 分布式锁 / 限流工具 |
| `DistributedLockAspect` | redisson-api 在 classpath | @DistributedLock 切面 |
| `BusinessMetrics` | micrometer-core 在 classpath | 业务指标 (actuator) |

**关键经验**:
- `@ConditionalOnClass(Class<?>)` 会触发编译期依赖 (类必须存在)
- 用 `@ConditionalOnClass(name = "FQCN")` 字符串形式 → 只在运行时检查, **编译期不需要 jar 存在**
- 这样 common 标 `<optional>true</optional>` 才能真正生效

