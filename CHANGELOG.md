# jvfault Framework Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
每个版本标签对应 roadmap 中的模拟纪元（2015→2026），git 提交使用真实日期。

---

## [v1.0.0] - Production Ready（纪元 2026）

### Added
- **security**: JWT HS256（JDK JCE 实现零依赖）、PBKDF2 口令哈希、`JwtGuard` 角色守卫
- **ops**: `HealthIndicator`/`HealthAggregator`（liveness/readiness 探针语义）、`GracefulShutdown`
- **compliance**: 只追加 JSONL `AuditTrail`、`DataMasker`（手机号/邮箱/通用掩码）
- **migration**: Spring → jvfault 重写规则表、源码扫描器、Markdown 迁移报告
- **distribution**: java-platform BOM 对齐全部构件版本
- **example**: v1.0.0 生产就绪全家桶演示

### 备注
- compliance/migration 为 2026 纪元模块，编译目标 JDK 17

---

## [v0.11.0] - AI Native（纪元 2025）

### Added
- **ai**: `ChatModel` SPI（函数式）、OpenAI 兼容客户端（JDK HttpClient + SSE 流式）、工具调用 schema
- **rag**: `VectorStore` SPI、内存向量存储（余弦相似度）、句子感知分块器、`RagPipeline`
- **mcp**: Model Context Protocol JSON-RPC 服务器（initialize/tools/resources）
- AI 时代模块编译目标 JDK 17

---

## [v0.10.0] - Native & Structured Logging（纪元 2024）

### Added
- **logging**: `LogEntry`（MDC trace 透传）、JSON/PlainText 格式化器、`StructuredLogger`（先拦截后格式化）、运行时级别注册表
- **native**: `NativeRuntimeHints`（reflect-config 聚合、native 模式探测）

---

## [v0.9.0] - Plugin & Compile-time（纪元 2023）

### Added
- **plugin**: ServiceLoader 发现、URLClassLoader 隔离、依赖拓扑排序、状态机生命周期
- **apt**: 编译期模块元数据注解处理器（META-INF/jvfault/modules.txt）
- **aot**: GraalVM reflect-config 生成器

---

## [v0.6.0] - Microservices（纪元 2020）

### Added
- **microservices**: `Message` 信封 + JSON 编解码、`PatternMatcher`（*/# 通配）、
  Transport SPI、`InMemoryTransport`、注解处理器（@MessageHandler/@EventPattern）、客户端代理工厂
- **transport-tcp**: JDK 完整实现（4 字节帧协议 + 端到端测试）
- **transport-{grpc,kafka,redis,nats,rmq,mqtt}**: 配置校验 + 惰性生命周期 + 客户端库适配骨架

---

## [v0.7.0] - API & Observability（纪元 2021）

### Added
- **openapi**: 从路由表生成 OpenAPI 3.0 文档（JSON/YAML，路径参数转换）
- **graphql**: schema-first 端点（graphql-java 21）
- **scheduling**: `@Scheduled`（fixedDelay/fixedRate/cron）、6 字段 Cron 解析器（含英文缩写、
  标准 DOM/DOW 通配语义）、线程池调度器 + 重臂链
- **cache**: `Cacheable/@CachePut/@CacheEvict`（JDK 代理）、ConcurrentMap/Caffeine/多级缓存

---

## [v0.2.0 ~ v0.5.0] - AOP, Config, Web Stack（纪元 2016-2019）

### Added
- **aop**: `@Aspect` + 通知注解、execution pointcut 子集、JDK/ByteBuddy 双代理
- **config**: `Environment` 抽象、YAML/Properties/JSON 源、`@Value`/`@ConfigurationProperties`/`@Profile`
- **exception**: RFC 7807 `ProblemDetail`、HTTP 异常层次、`@ExceptionHandler` 注册表
- **validation**: JSR-380 风格约束子集、级联校验、自定义校验器 SPI
- **web**: `@Controller` + 动词注解、路由（:param/*/字面优先）、Guard/拦截器管道、参数绑定
- **platform-servlet**: Servlet 5.0 桥接（请求/响应 + 分发 Servlet）
- **websocket**: `@WebSocketGateway`/`@SubscribeMessage`、房间广播
- **sse**: 规范事件流 `SseEmitter` + 通道 Hub
- **tracing**: W3C traceparent 传播、Span/Tracer、导出器 SPI
- **metrics**: Counter/Timer/Gauge/Summary、MeterFilter、JSON 快照

---

## [v0.1.0] - Foundation（纪元 2015）

### Added
- **core**: `DefaultBeanRegistry`（单例/原型/请求作用域、三级缓存循环依赖、FactoryBean、
  父子容器）、`ModuleContainer`（模块图 + 拓扑排序）、`ModuleScanner`（ClassGraph）、
  `JvfaultApplication` 引导、`BeanPostProcessor` 扩展点
- **test**: JUnit 5 扩展（`@TestModule` + `@Autowired` 字段/参数注入）
- **示例**: v0.1.0 核心应用

### Technical
- JDK 8 基线（`--release 8` 编译）
- ClassGraph 4.8.168、jakarta.inject 2.0.1、jakarta.annotation 2.1.1、SLF4J 2.0
- 构建零外部插件（纯 Gradle 内置 java-library/maven-publish/jacoco）

---

## 工程决策记录

- 2026-09-19: 全框架构建验证通过（36 个测试模块 239 个测试全绿，全部 10 个示例可运行）
- 2026-09-19: 框架从 monorepo 拆分为独立仓库（rwx-robot/jvfault）
- 2026-09-19: 构建"纯 Java 化" — 移除 Kotlin/Shadow/Checkstyle 插件依赖（ADR-001）
- 2026-09-19: transport-tcp 采用 JDK NIO 而非 Netty（ADR-007，零依赖基线）
