# jvfault Framework

> **Modern Modular Java Framework Architecture** — 模块化、类型安全的纯 Java 框架。
> 基线 JDK 8（`--release 8` 编译），遵循 JSR-330 / JSR-250 / SPI 标准。

[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-green.svg)](https://gradle.org/)
[![Tests](https://img.shields.io/badge/tests-239%20passing-brightgreen.svg)](#构建与测试)

## 快速开始

```bash
# 构建全部 38 个模块（36 个含测试，共 239 个测试）
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build

# 单模块
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :core:test

# Maven 本地安装
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew publishToMavenLocal
```

## 核心概念

| 组件 | 说明 |
|------|------|
| `@Component` / `@Module` | Bean 与模块声明 |
| `@Inject` / `@Named` | JSR-330 注入 |
| `BeanRegistry` | 三级缓存循环依赖、FactoryBean、父子容器 |
| `BeanPostProcessor` | 初始化序列扩展点（AOP/配置/调度接入点） |
| `WebApplication` | 路由 → Guard → 拦截器 → RFC 7807 |

## 模块总览

```
core        IoC 容器、模块系统、类路径扫描、引导
aop         @Aspect 切面、execution pointcut、JDK/ByteBuddy 代理
config      Environment、YAML/Properties/JSON、@Value/@ConfigurationProperties
validation  JSR-380 风格约束校验（级联 + 自定义校验器 SPI）
exception   RFC 7807 ProblemDetail、@ExceptionHandler 注册表
logging     结构化 JSON 日志、MDC trace 透传、动态级别
web         @Controller 路由、Guard/拦截器管道、参数绑定
platform-servlet   Servlet 5.0 桥接
platform-reactive  Reactor Mono/Flux 渲染
websocket   @WebSocketGateway 网关与房间广播
sse         规范事件流 SseEmitter / SseHub
openapi     OpenAPI 3.0 文档生成
graphql     graphql-java schema-first 端点
security    JWT HS256、PBKDF2、鉴权 Guard
microservices      Transport SPI、@MessageHandler、客户端代理
transport-{tcp,grpc,kafka,redis,nats,rmq,mqtt}   传输适配
tracing     W3C traceparent、Span/Tracer
metrics     Counter/Timer/Gauge/Summary
scheduling  @Scheduled + 6 字段 Cron
cache       @Cacheable/@CacheEvict、多级缓存
apt/aot/native     编译期元数据、GraalVM 反射配置
ai/rag/mcp  ChatModel SPI、向量检索、MCP 服务器   (JDK 17)
compliance/migration/ops           审计脱敏、迁移分析、健康检查
distribution                       版本对齐 BOM (java-platform)
test        JUnit 5 扩展 (@TestModule + @Autowired)
```

## 与 Monorepo 的关系

本仓库是框架本体；示例应用、架构文档与参考资料位于上层 monorepo
（`jvfault-all`），通过组合构建（`includeBuild` + 依赖替换）消费本仓库。

## License

Apache License 2.0 — see [LICENSE](LICENSE)
