# jvfault 架构决策记录 (ADR)

> 本文件记录框架的关键技术决策。日期均为实际决策日期；版本纪元对应 roadmap 中的模拟时间线。

## ADR-001: 纯 Java 构建，不引入 Kotlin
- **状态**: 已采纳 (2026-09-19)
- **背景**: 最初计划 "Kotlin + Java 混合"。实际代码全部为 Java，且 Kotlin 插件带来额外的工具链下载与构建脆弱性。
- **决策**: 框架 100% Java（JDK 8 API 基线），构建仅用 Gradle 内置插件（java-library / maven-publish / jacoco），不使用 Kotlin、Shadow、Checkstyle、SpotBugs 等外部插件。
- **影响**: 构建零外部插件依赖，可离线构建；"纯粹的完整 Java 框架目录" 目标达成。

## ADR-002: JDK 编译策略
- **状态**: 已采纳
- **决策**: 构建统一运行在 JDK 21 上；框架模块默认 `--release 8`（2015 基线），AI 时代模块（ai/rag/mcp）在各自 build.gradle.kts 中提升为 `--release 17`。
- **影响**: 早期模块保持最大兼容性；晚期模块可用 JDK 11+ API（如 java.net.http.HttpClient）。

## ADR-003: 不使用 module-info.java（v0.x 阶段）
- **状态**: 已采纳
- **背景**: 早期草稿包含 module-info.java，但与 `--release 8` 基线冲突（JPMS 自 JDK 9 起）。
- **决策**: v0.x 阶段移除 module-info；JPMS 支持推迟到 JDK 11+ 纪元的模块（若需要）。

## ADR-004: BeanPostProcessor 作为容器扩展点
- **状态**: 已采纳 (2026-09-19)
- **决策**: core 提供 `com.jvfault.core.container.BeanPostProcessor`（before → @PostConstruct/initMethod → after 初始化序列；after 可返回代理替换实例）。BPP 实现本身是普通 Bean，容器在创建其他 Bean 前先实例化全部 BPP。
- **影响**: aop（代理包装）、config（@Value/@ConfigurationProperties 注入）、validation、scheduling（@Scheduled 注册）等模块全部通过该钩子接入，无需侵入 core。

## ADR-005: Monorepo 布局与组合构建
- **状态**: 已采纳 (2026-09-19)
- **决策**:
  - `jvfault/` — 框架本体：**独立可构建**的纯 Java 多模块 Gradle 工程（38 个模块）。
  - `examples/`、`tests/` — 根构建承载，通过 `includeBuild("jvfault")` + 显式依赖替换消费 `com.jvfault:jvfault-*` 构件，无需发布到仓库。
  - `.jvfault-memory/` — 跨会话知识库（git 跟踪）；`.jvfault-state/` — 运行时状态（git 忽略）。

## ADR-006: 依赖选型（Java 8 兼容约束）
| 用途 | 选择 | 版本 | 备注 |
|------|------|------|------|
| DI 规范 | jakarta.inject-api | 2.0.1 | JSR-330 |
| 生命周期 | jakarta.annotation-api | 2.1.1 | JSR-250 |
| 类路径扫描 | classgraph | 4.8.168 | 弃用 ASM 直用（扫描器只用 ClassGraph） |
| AOP 字节码 | byte-buddy | 1.14.9 | 类代理；接口代理用 JDK Proxy |
| YAML | snakeyaml | 2.2 | config 模块 |
| JSON | jackson-databind | 2.17.0 | config/microservices/openapi |
| 本地缓存 | caffeine | **2.9.3** | 3.x 需 Java 11，弃用 |
| 序列化/网格 | — | — | tracing/metrics 自研轻量实现，暂不依赖 OpenTelemetry/Micrometer（保留桥接可能） |
| Servlet | jakarta.servlet-api | 5.0.0 | EE9，Java 8 兼容（EE10 需 Java 11） |

## ADR-007: 传输层实现策略
- **状态**: 已采纳
- **决策**: transport-tcp 采用 JDK NIO（零额外依赖）；grpc/kafka/redis/nats/rmq/mqtt 分别基于官方客户端库。若依赖与 Java 8 基线冲突，选择最后一个 Java 8 兼容版本并在模块 Javadoc 注明。
- **影响**: 与原计划 "TCP 用 Netty" 不同 —— 为保持零依赖基线与可测试性，Netty 适配器可作为后续可选模块。

## ADR-008: 版本时间线的呈现
- **状态**: 已采纳
- **决策**: 12 个版本标签（v0.1.0→v1.0.0）对应 2015-2026 的**模拟纪元**，用于设计叙事与 JDK/依赖演进。git 提交使用真实日期，不伪造历史；`scripts/simulate-git-history.*` 保留供有需要时生成美化历史。


## ADR-009: 框架独立仓库
- **状态**: 已采纳 (2026-09-19)
- **决策**: 框架本体从 monorepo 拆分为独立仓库 `rwx-robot/jvfault`；
  示例（examples/）作为框架构建的普通子模块随仓库分发；
  monorepo 仅保留 Agent 工作区与跨项目材料。
- **影响**: 单仓库自包含：构建、测试、示例一条命令完成。
