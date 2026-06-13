# Redisson 分布式工具

> 状态:✅ 已集成(`redisson-spring-boot-starter:3.34.1`,覆盖 Spring Boot 自带的 3.27.2)

## 解决什么问题

Spring Data Redis 只给 `RedisTemplate` / `StringRedisTemplate`,但实际分布式场景我们经常需要:

- 🧱 **分布式锁** — 防止同一资源被并发操作(同一智能体不能被多用户同时触发)
- 🚦 **分布式限流** — 按用户/资源细粒度令牌桶(比 Sentinel 网关更细)
- 🧮 **分布式计数器** — 实时调用量、UV/PV
- 📚 **分布式集合** — 跨节点共享的 Map / List / Queue
- 📡 **Pub/Sub** — 跨节点事件广播(Flowable 任务回调、状态变更)
- ⏰ **延迟队列** — 延时任务(订单超时、定时提醒)

Redisson 把这些都封装好了,API 友好,内置看门狗自动续期。

## 能力地图

| 能力 | 类 | 备注 |
|---|---|---|
| 分布式可重入锁 | `RLock` | 默认实现 |
| 公平锁 | `RFairLock` | 按请求顺序排队 |
| 读写锁 | `RReadWriteLock` | 读多写少场景 |
| 分布式限流器 | `RRateLimiter` | 令牌桶 |
| 原子计数器 | `RAtomicLong` | 可过期 |
| 分布式集合 | `RSet/RList/RMap/RQueue/RBlockingQueue` | |
| Pub/Sub | `RTopic` | |
| 延迟队列 | `RDelayedQueue` | |
| 分布式限流 | `RSemaphore` | 信号量 |
| 布隆过滤器 | `RBloomFilter` | 去重 |

## 怎么用

### 1. 注入 RedissonUtil(快捷方式)

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final RedissonUtil redisson;

    public void demo() {
        // 分布式锁
        String r = redisson.executeWithLock("my-key", 3, 30,
            () -> doSomething());

        // 分布式限流
        if (!redisson.tryAcquire("user:" + uid, 10, 60)) {
            throw new RuntimeException("限流");
        }

        // 计数器
        long calls = redisson.incr("stats:calls");
    }
}
```

### 2. 用 @DistributedLock 注解(推荐)

```java
@DistributedLock(
    key = "agent:chat:#{#agentId}",  // SpEL
    prefix = "lock:",
    waitTime = 5,    // 等待 5s
    leaseTime = 60   // 自动 60s 后释放
)
public String chat(Long agentId, String input) { ... }
```

切面自动加锁/释放,业务代码零侵入。

### 3. 直接用 RedissonClient(高级)

```java
@Service
@RequiredArgsConstructor
public class Advanced {
    private final RedissonClient redisson;

    public void useSet() {
        RSet<String> set = redisson.getSet("set:online-users");
        set.add("alice");
        set.contains("alice"); // true
    }

    public void usePubSub() {
        RTopic topic = redisson.getTopic("topic:agent-events");
        topic.addListener(String.class, (ch, msg) -> {
            System.out.println("收到: " + msg);
        });
        topic.publish("agent:1 status changed");
    }

    public void useDelayedQueue() {
        RQueue<String> queue = redisson.getQueue("dq:reminders");
        RDelayedQueue<String> dq = redisson.getDelayedQueue(queue);
        dq.offer("30秒后提醒用户", 30, TimeUnit.SECONDS);
    }
}
```

## 配置(application.yml)

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:           # 留空 = 无密码
    database: 0
    timeout: 3000ms
```

更复杂配置(集群/哨兵/SSL)用 `redisson.config` JSON 覆盖:

```yaml
redisson:
  config: |
    {
      "singleServerConfig": {
        "address": "redis://localhost:6379",
        "password": null,
        "database": 0,
        "connectionPoolSize": 32,
        "connectionMinimumIdleSize": 8
      }
    }
```

## 跟 Sentinel / 本地锁的对比

| 维度 | 本地 `synchronized` | Redisson 分布式锁 | Sentinel 限流 |
|---|---|---|---|
| 范围 | 单 JVM | 跨集群 | 网关/方法 |
| 性能 | 最高 | 中(网络开销) | 高(本地内存) |
| 公平性 | 取决于 JVM | 支持 fairLock | 无 |
| 自动续期 | 无 | 看门狗 10s | 无 |
| 死锁防护 | 弱 | leaseTime 自动释放 | 不涉及 |
| 适用 | 单机内部 | 跨节点串行化 | 流量整形 |

**经验**:Sentinel 用于"流量入口粗粒度限流",Redisson 用于"业务内细粒度串行化"。

## 启动 Redis(本地)

```bash
docker run -d -p 6379:6379 --name redis \
  -v $(pwd)/sql/redis-data:/data \
  redis:7.2-alpine redis-server --appendonly yes
```

或者用项目里的 docker-compose,`redis` 服务已经包含。
