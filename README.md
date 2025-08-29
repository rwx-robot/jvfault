# jvfault Framework

> **Modern Modular Java Framework Architecture** — 注解驱动、模块化、类型安全的纯 Java 框架。
> 基线 JDK 8（`--release 8` 编译），遵循 JSR-330 / JSR-250 / SPI 标准，零外部框架依赖。

[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-green.svg)](https://gradle.org/)
[![Tests](https://img.shields.io/badge/tests-269%20passing-brightgreen.svg)](#构建与测试)
[![CI](https://github.com/rwx-robot/jvfault/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/rwx-robot/jvfault/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/release-v1.0.7-blue.svg)](#)

**jvfault** 是一个以 Java 标准库实现的模块化应用框架：IoC 容器 + 模块化系统为内核，
上层覆盖 Web（Servlet/Reactive）、安全、合规、可观测性、微服务传输与 AI 接入。
设计风格借鉴主流注解驱动框架，但**只用 JDK 与少量成熟客户端库**，不引入 Spring / Kotlin / 其他语言生态。

| 维度 | 现状 |
|------|------|
| 模块 | 39 个（38 个含测试） |
| 版本示例 | 12 个（v0.1.0 → v1.0.0 每版本一个可运行示例） |
| 测试 | **269 个，0 失败 0 跳过**（含 6 例 JPMS 多版本 JAR 回归） |
| 传输适配 | tcp / grpc / kafka / redis / rmq / nats / mqtt（**全部真实集成**：broker 或 netty 回环） |
| JPMS | **38 个模块** 提供多版本 JAR 的 `META-INF/versions/9/module-info.class`（Java 8 与 9+ 双向兼容） |
| 构建 JVM | 必须用 JDK 21 作 Gradle JVM（`JAVA_HOME=$(/usr/libexec/java_home -v 21)`） |
| CI | GitHub Actions：5 个真实 broker（redis / kafka / rabbitmq / nats / mosquitto）+ gRPC 回环；18 个传输集成测试在 CI 上实跑 |

## 快速开始

```bash
# 构建全部 39 个模块 + 12 个版本示例（38 个模块含测试，共 269 个测试）
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build

# 单模块
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :core:test

# 运行示例（v0.1.0 - v1.0.0 每版本一个）
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :examples:v1.0.0:run

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
security    JWT HS256、PBKDF2 口令哈希、JwtGuard
microservices      Transport SPI、@MessageHandler、客户端代理
transport-{tcp,grpc,kafka,redis,nats,rmq,mqtt}   传输适配（7 个均含真实集成测试）
tracing     W3C traceparent、Span/Tracer
metrics     Counter/Timer/Gauge/Summary
scheduling  @Scheduled + 6 字段 Cron
cache       @Cacheable/@CacheEvict、多级缓存
plugin      插件 SPI：隔离 ClassLoader、依赖拓扑、生命周期
apt         编译期 @Module 元数据生成（META-INF/jvfault/modules.txt）
aot/native  GraalVM 反射配置生成与 native 提示
virtualthreads     虚拟线程执行器与结构化并发
ai/rag/mcp  ChatModel SPI、向量检索、MCP 服务器   (JDK 17)
compliance/migration/ops           审计脱敏、迁移分析、健康检查与优雅关闭
test        JUnit 5 扩展 (@TestModule + @Autowired)
tests       跨模块端到端套件（Jetty + JWT）
```

## 集成测试与 CI

传输模块的集成测试连接真实服务，地址来源优先级：

1. 环境变量 `JVFAULT_REDIS_BOOTSTRAP` / `JVFAULT_KAFKA_BOOTSTRAP` / `JVFAULT_RMQ_BOOTSTRAP`
   / `JVFAULT_NATS_BOOTSTRAP` / `JVFAULT_MQTT_BOOTSTRAP`（CI 由 GitHub Actions `services` 提供）
2. 本地 Docker 容器（`redis:7.2-alpine`、`apache/kafka:3.7.0` KRaft 单节点、`rabbitmq:3-management`、
   `nats:2.10-alpine`、`eclipse-mosquitto:1.6`）
3. 两者都不可用 → 该集成测试跳过（不影响构建绿）

环境变量指向的 broker 会先做 **TCP 可达性探测**（30s 重试窗口），不可达则跳过 ——
这样 CI service 容器处于「端口已映射但应用未就绪」的窗口期时不会把构建拖红。

CI 上 **5 个 broker 全部以真实 service 容器接入**（redis / kafka / rabbitmq / nats / mosquitto），
gRPC 走本机回环，因此 **18 个传输集成测试在 CI 上真实执行**（不再跳过）。
Kafka 的 service 健康检查必须写全路径 `/opt/kafka/bin/kafka-broker-api-versions.sh`
（该脚本不在镜像 PATH 上，写裸命令会让容器被判 unhealthy）。

### 本地复现 CI 环境

CI runner 是 **UTC**，本地通常是 UTC+8 —— 时间戳相关断言可能在本地过、CI 挂。
改完时间/日期相关代码后，用下面命令复验：

```bash
TZ=UTC ./gradlew clean build --no-daemon --no-build-cache
```

`--no-build-cache` 是为了避免缓存命中掩盖问题。

gRPC 无需外部服务：服务端以 `grpc-netty` 绑定随机端口（本机回环），集成测试直接与之通信。

## 工作区关系

本仓库是框架本体；上层 Agent 工作区（`jvfault-all`）只保留参考资料与运行状态，不再跟踪框架代码。

## License

Apache License 2.0 — see [LICENSE](LICENSE)
