# jvfault Framework Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
每个版本标签对应 roadmap 中的模拟纪元（2015→2026），git 提交使用真实日期。

---

## [v1.0.7] - Kafka 纳入 CI：5 个 broker 全覆盖，18 个集成测试全实跑（2026-09-23）

### Fixed
- **ci**：Kafka 进不了 CI 的真因 —— `kafka-broker-api-versions.sh` **不在容器 PATH 上**，
  位于 `/opt/kafka/bin/`。健康检查写裸命令 → `command not found` → 容器被判 unhealthy →
  GitHub `Initialize containers` 步直接失败，整轮 CI 挂。
  改为写全路径 `/opt/kafka/bin/kafka-broker-api-versions.sh`，
  且 `--bootstrap-server` 用**容器内监听地址** `localhost:9092`（不是宿主机映射端口）。
- **docs**：README 的 CI badge 是**静态假徽章**（`badge/CI-passing`），
  CI 真红时仍显示 passing。改为 GitHub Actions 实时徽章
  `.../actions/workflows/ci.yml/badge.svg?branch=main`。
- **docs**：README 集成测试数误写 28，实为 **18**（6 个传输模块 × 3）；删除重复的 badge 块。

### Added
- **ci**：Kafka service 回归（redis / kafka / rabbitmq / nats / mosquitto 五个真实 broker）。
  CI 上 **18 个传输集成测试全部真实执行，不再跳过**。
- **docs**：README 补「本地复现 CI 环境」指引 —— CI runner 是 UTC、本地多为 UTC+8，
  改完时间相关代码必须用 `TZ=UTC ./gradlew clean build --no-daemon --no-build-cache` 复验。
- **docs**：能力表补 CI 行。

### Changed
- **build**：构件版本 `jvfaultVersion` 由 `1.0.6` 升到 `1.0.7`。

### Verification
- 本机同镜像同参数实测 Kafka 健康检查：`docker inspect` → `health=healthy`（约 10s）
- `JVFAULT_KAFKA_BOOTSTRAP=localhost:9092 ./gradlew :transport-kafka:test`
  → 3 tests 0 skipped 0 failures
- **全量 CI 等价复现**（`TZ=UTC` + 5 broker + `--no-build-cache`）：
  **269 tests 0 failures 0 errors 0 skipped**，其中传输集成 **18/18 全跑**
- GitHub Actions 连续两轮 success

---

## [v1.0.6] - CI 首次全绿（修复 4 类"只在干净环境暴露"的缺陷）（2026-09-23）

> 本轮起 CI（GitHub Actions）连续为绿。此前所有失败都源于**只有干净环境才会触发**
> 的缺陷 —— 本地因构建产物/镜像/时区已就位而看不出来。

### Fixed
- **logging**：`PlainTextFormatterTest` 时间戳正则不接受 `Z` 偏移。
  `DateTimeFormatter` 的 `XXX` 在零偏移时按 ISO-8601 渲染为 `Z`（CI runner 为 UTC），
  非零偏移渲染为 `+08:00`（本机 UTC+8）→ 本地过、CI 挂。
  正则改为 `(?:[+-]\d{2}:?\d{2}|Z)`。
- **transport-\***：`DockerContainer.ensureImage()` 用「stdout 是否为空」判断镜像
  是否存在，但 `exec()` 合并了 stderr —— 镜像不存在时 docker 把
  `Error response from daemon: No such image` 写到 stdout，判空为 false →
  `hasImage()` 恒返回 true → 测试去 `docker run` 一个不存在的镜像 → 起不来 →
  空转 2 分钟后 `@BeforeAll` 抛错，整个测试类失败（CI runner 无镜像，必现）。
  改为**判退出码**：新增 `execCapture/execExitCode`；`start()` 亦校验 `docker run`
  退出码，若失败但端口已有 broker 在监听则复用（`containerId` 留 null，
  `close()` 不会误删外部容器）；`transport-redis` 的 `dockerAvailable()` 补显式判空。
- **transport-\***：拿到 `JVFAULT_*_BOOTSTRAP` 后直接 `brokerUp = true`，不做可达性
  探测。CI service 容器"端口已映射但应用未就绪"或本机未起 broker 时，
  测试真连 → `TransportException` → 整个类失败。
  新增 `isReachable(scheme://host:port)` TCP 探测（30s 重试窗口），不可达则
  `brokerUp = false` 交由 `assumeTrue` 跳过；Kafka 的 `waitForBroker` 由"抛错"改为
  "未就绪则跳过"。
- **tests**：`:tests:test` 未依赖全量模块 jar。`JpmsModulePathSmokeTest`
  遍历 38 个带 `module-info.java` 的模块校验各自的 jar，但 `:tests` 只声明了
  3 个依赖 → 干净检出（CI 每次都是）时其余 ~35 个 jar 尚未产出 → 失败。
  改为 `:tests:test.dependsOn(<每个子项目>:jar)`（任务路径字符串惰性解析）；
  并让 `verifyMrJarDescriptors` 在"发现 0 个模块"时明确失败，避免 `moduleRoot()`
  解析错时循环空转造成"假绿"。

### Added
- **ci**：新增 `release.yml` —— 推送 `v*` tag 时自动跑 `./gradlew jar`、从
  CHANGELOG 提取该版本章节作为 release notes、创建 GitHub Release 并附加全部
  构件（v1.0.5 已验证：40 个 jar 资产）。tag 含 `-rc/-alpha/-beta` 标记 prerelease。
- **ci**：`ci.yml` 增加 `concurrency`（同 ref 互斥，自动取消旧 run）、
  `permissions: contents: read`（最小权限）、`tags: ['v*']` 触发、
  JaCoCo 报告上传、`ubuntu-22.04` 固定版本。
- **build**：`check.dependsOn(jacocoTestReport)`，各模块构建时产出覆盖率 HTML 报告。

### Notes
- **CI broker 覆盖**：redis / rabbitmq / nats / mosquitto 以 GitHub service 容器接入；
  gRPC 走本机回环。Kafka 暂未纳入 CI（KRaft 冷启动 + 缺少可靠的 service health check
  会让 `Initialize containers` 误判失败），其集成路径由本地 + Docker 通道覆盖。
- 新增 skill `gradle-jpms-mrjar`：沉淀「Gradle 多模块 Java 8 基线 + JPMS 多版本 JAR」
  的做法与 6 个高频陷阱。

### Changed
- **build**：构件版本 `jvfaultVersion` 由 `1.0.5` 升到 `1.0.6`。

---

## [v1.0.5] - JPMS 回归测试加固（消除静默失败）（2026-09-23）

### Fixed
- **tests**: `JpmsModulePathSmokeTest` 三个静默失败
  - **版本硬编码 1.0.3**：通过 `tests/build.gradle.kts` 注入 `-Djvfault.framework.version`
    系统属性，避免在 1.0.4+ 上静默跳过所有 jar 校验
  - **模块列表硬编码**：改为 `Files.newDirectoryStream(<root>)` 自动发现所有
    `src/main/java9/module-info.java`，新增模块自动纳入
  - **jar 缺失静默跳过**：改为失败累加器，缺失则 fail
- **tests**: 新增 `verifyModuleNameInSource` —— 源码层校验
  `module-info.java` 里的 `module com.jvfault.X` 与项目自动模块名一致
  （捕获 `:native` 项目名是保留字之类的静默错误）
- **tests**: 新增 `verifyExamplesStructure` —— 校验 12 个 examples/v* 目录
  + 每个含 `Application.java`（防止 examples 目录被切走仍声称 12/12 绿）
- **build**: `tests/build.gradle.kts` 通过 `rootProject.projectDir.absolutePath`
  注入 `-Djvfault.project.root`，解决 `:tests:test` 的 `user.dir` 是 tests 模块目录
  而非项目根的问题

### Added
- **tests**: 常驻 JPMS 回归 6 例（267 → 269 测试）

### Changed
- **build**: 构件版本 `jvfaultVersion` 由 `1.0.4` 升到 `1.0.5`

---

## [v1.0.4] - JPMS 全面覆盖（2026-09-23）

### Added
- **module-info**: 30 个新模块补 `src/main/java9/module-info.java`：
  `aop` / `apt` / `cache` / `compliance` / `config` / `graphql` / `logging` / `mcp` /
  `metrics` / `migration` / `microservices` / `ops` / `openapi` / `platform-reactive` /
  `platform-servlet` / `plugin` / `rag` / `scheduling` / `security` / `sse` / `test` /
  `tracing` / `transport-{tcp,grpc,kafka,redis,rmq,nats,mqtt}` / `validation` /
  `virtualthreads` / `web` / `websocket` / `ai` — **合计 38 个 MR-JAR 模块**（剩余 2 个
  `tests`/`examples` 为非业务构件，无 MR-JAR 必要）
- **build/JPMS 改进**：根 `build.gradle.kts` 解析上游项目 jar 任务作为 module path（不依赖
  classes 目录）、新增独立 `compileJava9ModulePath` configuration 仅用于解析三方 jar；
  `:native` 模块名用硬编码特例（项目名是 Java 保留字）；`:security` 仅导出子包
- **tests**: 新增 `JpmsModulePathSmokeTest`（4 例），常驻回归：
  - MR-JAR 描述符完整（30 个核心模块 manifest + `META-INF/versions/9/module-info.class`）
  - `JvfaultApplication.getVersion()` 在 classpath / module path 下都可读
  - 当前测试 Module 标识可读（命名 / 未命名均可）

### Changed
- **build**: 构件版本 `jvfaultVersion` 由 `1.0.3` 升到 `1.0.4`

### Added
- **build/JPMS**: 多版本 JAR（MR-JAR）机制 —— 根 `build.gradle.kts` 在模块提供
  `src/main/java9/module-info.java` 时，以 `--release 9` **单独**编译该描述符
  （`--patch-module` 打补丁 + 依赖以 JAR 形态作为 module path），产物放入
  `META-INF/versions/9/` 并置 `Multi-Release: true`；**主代码仍以 `--release 8` 编译**
- **module-info**: 首批 4 个模块 —— `com.jvfault.core` / `.exception` / `.logging` / `.metrics`；
  `api` 依赖映射为 `requires transitive`，项目依赖以 JAR 上 module path

### 说明
- 同一构件**双向兼容**：Java 8 走 `Automatic-Module-Name`，Java 9+ 走
  `META-INF/versions/9/module-info.class`
- 已用**模块化消费者**实测：`requires com.jvfault.core` + `com.jvfault.exception` 的独立模块
  可在 module path 上编译并运行，IoC 容器跨模块反射实例化 `@Component` 正常
- 主类字节码 major 52（Java 8），描述符 major 53（Java 9）

### Changed
- **build**: 构件版本 `jvfaultVersion` 由 `1.0.2` 升到 `1.0.3`

---

## [v1.0.2] - 传输层真实集成补全 + JPMS 第一步（2026-09-22）

### Added
- **transport-rmq**: RabbitMQ 真实实现（amqp-client 5.20.0）—— topic exchange 按 pattern 绑定、
  `replyTo`/`correlationId` 请求-响应、handler 异常经 error 头回传；新增 `RabbitMQWire`、
  `DockerContainer` + `RabbitMQIntegrationTest`（3 例真实 broker）
- **transport-nats**: NATS 真实实现（jnats 2.17.5）—— NATS core request/reply（库自带 inbox）、
  `#` → `>` 通配翻译；新增 `NATSWire`、`DockerContainer` + `NATSIntegrationTest`（3 例真实 broker）
- **transport-mqtt**: MQTT 真实实现（Eclipse Paho mqttv3 1.2.5）—— 独占 replyTopic + correlationId
  请求-响应、`*` → `+` 通配翻译；新增 `MqttWire`、`DockerContainer` + `MQTTIntegrationTest`（3 例真实 broker）
- **transport-grpc**: gRPC 真实实现（grpc-netty 1.62.2）—— 通用 unary `MethodDescriptor` + `byte[]`
  直通编组（无需 protobuf 代码生成）；新增 `GrpcWire`/`GrpcMethod`、`GrpcIntegrationTest`（3 例，netty 本机回环）
- **ci**: 新增 rabbitmq / nats / mosquitto 三个 `services`，以及
  `JVFAULT_RMQ_BOOTSTRAP` / `JVFAULT_NATS_BOOTSTRAP` / `JVFAULT_MQTT_BOOTSTRAP` 环境变量
- **build/JPMS**: 40 个主构件写入 `Automatic-Module-Name: com.jvfault.<name>`（Java 8 兼容的
  JPMS 第一步）；`module-info.java` 与 `--release 8` 基线冲突，留待多版本 JAR 决策

### Changed
- **transport**: rmq / nats / mqtt / grpc 由「配置校验 + 惰性生命周期 + 客户端库适配骨架」升级为
  **真实实现**（真实 broker / netty 回环端到端验证），测试数 251 → 263
- **build**: 构件版本 `jvfaultVersion` 由 `1.0.1` 升到 `1.0.2`

### Fixed
- README / HANDOVER 数据同步：传输适配 7 个全部真实集成、263 测试

---

## [v1.0.1] - 示例闭环与集成测试真实性（2026-09-22）

### Added
- **examples**: 补齐 `v0.9.0`（plugin 生命周期 / apt 编译期元数据 / aot 反射注册）与 `v1.0.0`（security / compliance / migration / ops）两个可运行示例，版本示例闭环 12/12
- **transport-kafka**: `KafkaIntegrationTest` 真实 broker 集成 3 例（request-reply、error 头回传、事件 publish），支持 CI service 与本地容器双通道
- **ci**: GitHub Actions `services` 提供 redis 7.2 与 kafka 3.7（KRaft 单节点）；新增 `publishToMavenLocal` 校验步骤与测试报告上传
- **apt**: 新增真实 javac 编译测试，验证 `META-INF/jvfault/modules.txt` 生成与处理器 SPI 注册

### Fixed
- **build**: 构件版本 `jvfaultVersion` 由 `0.1.0` 升到 `1.0.1` —— 此前发布的 40 个构件版本与 git tag / roadmap 完全脱节
- **core**: `JvfaultApplication.getVersion()` 兜底值不再硬编码 `0.1.0`，改为读取构建期生成的 `META-INF/jvfault-build.properties`
- **apt**: 补 `META-INF/services/javax.annotation.processing.Processor` 注册 —— 此前消费者挂 `annotationProcessor` 时处理器从未被 javac 发现
- **transport-kafka**: `close()` 改由轮询线程关闭 `KafkaConsumer`（此前跨线程关闭抛 `ConcurrentModificationException`）
- **transport-kafka**: 无任何订阅时 `bind()` 不再调用 `consumer.poll()`（此前抛 `IllegalStateException`）

### Changed
- README / HANDOVER 数据同步：39 个模块、12 个版本示例、251 测试；移除已删除的 distribution BOM 描述

---

## [v1.0.0] - Production Ready（纪元 2026）

### Added
- **security**: JWT HS256（JDK JCE 实现零依赖）、PBKDF2 口令哈希、`JwtGuard` 角色守卫
- **ops**: `HealthIndicator`/`HealthAggregator`（liveness/readiness 探针语义）、`GracefulShutdown`
- **compliance**: 只追加 JSONL `AuditTrail`、`DataMasker`（手机号/邮箱/通用掩码）
- **migration**: Spring → jvfault 重写规则表、源码扫描器、Markdown 迁移报告
- **distribution**: java-platform BOM 对齐全部构件版本（后按 ADR-009 移除，改由 version catalog 对齐）
- **example**: v1.0.0 生产就绪全家桶演示（示例工程于 v1.0.1 补齐）

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
