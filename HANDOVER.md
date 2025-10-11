# HANDOVER — jvfault Java 框架会话交接

**会话时间**: 2026-09-22
**最终状态**: 12 标签 roadmap 全部实现、CI/测试套件/真实传输/Maven 发布链路已落地，git 历史按版本纪元重建并强推至远程
**接手给**: 下一位 Agent（建议：同会话串接继续，或以本工作区为起点独立推进）

---

## 1. 一句话现状

`jvfault` 是一个注解驱动的模块化纯 Java 框架，**完整实现且全部测试通过**：
- 39 个框架模块 + 12 个版本示例（v0.1.0 - v1.0.0 每版本一个）
- 13 个 git commit（每个版本一个）+ 12 个 tag（v0.1.0 ~ v1.0.0 全含）
- **251 测试 0 失败 0 跳过**（含 7 个 Jetty+JWT 端到端 + 3 个 Redis 真实 broker + 3 个 Kafka 真实 broker 集成）
- 40 个构件已实测发布到本地 m2（`./gradlew publishToMavenLocal`）
- 远程仓库：`git@github.com:rwx-robot/jvfault.git`（仅 main 分支，身份 johnnynode <johnnynode@gmail.com>）

### 1.1 第二轮（2026-09-22 晚）修复与补全

| 项 | 问题 | 处理 |
|----|------|------|
| 示例闭环 | `settings.gradle.kts` 只注册 10 个示例，**缺 v0.9.0（plugin/apt/aot）与 v1.0.0（security/compliance/migration/ops）** | 新建两个可运行示例并注册，现 12/12 |
| apt 真失效 | 缺 `META-INF/services/javax.annotation.processing.Processor`，消费者挂 `annotationProcessor` 时处理器**从不运行** | 补注册 + 加真实 javac 编译测试，示例输出已非空 |
| CI 空跑 | CI 无 broker service，传输集成测试静默跳过 | 加 redis/kafka service + `JVFAULT_*_BOOTSTRAP` 环境变量契约 |
| kafka 真实集成 | 此前只有单元测试，未连过真 broker | 新增 `KafkaIntegrationTest`（3 例，真实容器/CI service 双通道） |
| Kafka bug A | `close()` 从调用线程关 consumer，与 poll 线程冲突 → `ConcurrentModificationException` | 改由轮询线程关闭 consumer，`close()` 只 wakeup + 等待 |
| Kafka bug B | 无订阅时 `bind()` 直接 `poll()` → `IllegalStateException` | 仅在有 handler 订阅时触发首次 poll |

## 2. 工作区结构

```
/Users/Wang/Code/webfault-lib/jvfault-all/
├── jvfault/          # ★ 独立 Git 仓库（已强推），框架本体
│   ├── core/ aop/ config/ validation/ exception/ logging/
│   ├── web/ platform-servlet/ platform-reactive/ websocket/ sse/
│   ├── openapi/ graphql/ security/
│   ├── microservices/ transport-{tcp,grpc,kafka,redis,nats,rmq,mqtt}/
│   ├── scheduling/ cache/ tracing/ metrics/
│   ├── plugin/ apt/ aot/ native/ virtualthreads/
│   ├── ai/ rag/ mcp/ compliance/ migration/ ops/
│   ├── test/ tests/ examples/v0.*/
│   ├── .github/workflows/ci.yml
│   ├── build.gradle.kts / settings.gradle.kts / gradlew
│   └── README.md / CHANGELOG.md / docs/architecture-decisions.md
├── .jvfault-memory/   # 本地（不入库）— 参考分析与阶段规划
├── .jvfault-state/    # 本地（不入库）— 当前 phase JSON
├── reference/         # 本地（不入库）— 外部参考资料
└── .gitignore / README.md / AGENT_HANDOFF.md  # 仅这三个文件入库
```

## 3. 关键命令（接手时立刻可跑）

```bash
# 全框架构建 + 245 测试
cd /Users/Wang/Code/webfault-lib/jvfault-all/jvfault
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build

# 单独模块测试
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :core:test :redis:test :web:test

# 完整发布到本地 m2
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew publishToMavenLocal

# 真实传输集成（需要 Docker 守护进程）
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :transport-redis:test
# 测试通过时实际启动 redis:7.2-alpine 容器跑 psubscribe+replyChannel+error 头全链路

# 强推远程（用户偏好：直接覆盖）
git remote add origin git@github.com:rwx-robot/jvfault.git
git push --force -u origin main
git push --force origin --tags
```

## 4. 架构与决策要点（详见 docs/architecture-decisions.md）

| ADR | 决策 |
|-----|------|
| 001 | 纯 Java（无 Kotlin / Shadow / Checkstyle），仅 Gradle 内置插件 |
| 002 | JDK 21 作 Gradle JVM，框架模块 --release 8，AI/2025 模块 --release 17 |
| 003 | 移除 module-info.java（v0.x 阶段无 JPMS 需求） |
| 004 | BeanPostProcessor 是 aop/config/scheduling 接入点 |
| 005 | jvfault/ 独立组合构建，父 monorepo 通过 dependencySubstitution 消费 |
| 006 | 依赖选型：ByteBuddy 1.14.9（aop 类代理 INJECTION 策略）、ClassGraph 4.8.168、caffeine 2.9.3、JUnit 5.10.2 |
| 007 | transport-tcp 用 JDK NIO（不引入 Netty） |
| 008 | 框架拆分为独立仓库，monorepo 退为 Agent 工作区 |
| 009 | distribution BOM 移除（与 java-platform 互斥且冗余，消费者用 Gradle version catalogs 对齐） |

## 5. 用户偏好（严格遵守）

- **内容红线**：仓库任何文件/任何提交**不得出现其他语言框架的名称对照表述**（含 javadoc "对应 XX:" 行；Spring 相关注释可保留）
- **git 偏好**：明确指示强推（`--force`），指定邮箱 `johnnynode@gmail.com`
- **构建 JVM 偏好**：必须用 JDK 21 作 Gradle JVM（`JAVA_HOME=$(/usr/libexec/java_home -v 21)`，系统默认 Java 是 12）
- **架构偏好**：注解驱动、模块化、类型安全；以 Java 标准（JSR-330/250/380、SPI）实现
- **语言偏好**：用户用中文沟通，文档与示例偏向现代框架术语

## 6. 接手时已确认的踩过的坑

1. **`String.trim()` 会吃掉 ≤ U+0020 的所有字符**：aop 之前用 `\u0002` 当跨段占位符，调用 `.trim()` 时被吞掉，**导致跨段通配失效**。修复：用三态匹配（"*" 占位独立判断，不依赖 trim）。
2. **ByteBuddy 包私有类代理**：默认策略抛 `IllegalAccessError`。**必须**用 `ClassLoadingStrategy.Default.INJECTION` 策略（见 aop/ByteBuddyClassProxy）。
3. **redis 客户端订阅连接禁止 PUBLISH**：必须**两条独立连接**——一条订阅，一条发布（详见 transport-redis/RedisTransportServer/Client）。
4. **Testcontainers 拉镜像超时**：本地网络受 Docker Hub 限流。**改用 docker CLI + 固定端口 `--publish=9092:9092`**（见 transport-redis/DockerContainer.java 模板）。
5. **ByteBuddy `*Service` 与 JDK 内部类同名冲突**：用 `@Override`+`new String[]{}` 避免。
6. **java-platform 与 java-library 互斥**：distribution BOM 模块需在 subprojects{} 排除。本项目**已删除 distribution 模块**（消费者用文档/version catalog 对齐版本）。
7. **kafka-clients 字段名以 `_CONFIG` 后缀结尾**：用 `ConsumerConfig.GROUP_ID_CONFIG`（不是 `GROUP_ID`）。
8. **ZonedDateTime 无 `plusMillis`**：用 `.plus(millis, ChronoUnit.MILLIS)`。
9. **jshell 不可用**（用户 macOS 默认 JDK 12）：统一用 `JAVA_HOME=$(/usr/libexec/java_home -v 21)` 切到 JDK 21。
10. **子代理有并发限制（同时只能跑 1 个）**：批量并行派发会报 "user concurrency limit exceeded"。**建议主线程亲自实现，不要批量派发子代理**。

## 7. 下一步建议（按优先级）

### 7.1 【高】CI 镜像预热
当前 `.github/workflows/ci.yml` 只跑 `./gradlew build`——**实际 CI 中需要**：
- 预拉 `redis:7.2-alpine`（已在 redis 测试里用）、`kafka:3.7.0`、`rabbitmq:3-management`、 `nats:2.10-alpine`、`eclipse-mosquitto:2.0`、`envoyproxy/envoy:v1.30`（GRPC stub）镜像
- 加 `services:` 段（docker-compose 形式）启动 broker，让 transport-redis 等的集成测试在 CI 中真跑
- 预期：CI 中 7 个传输 × 1~2 个测试 ≈ 14 个真实集成测试全跑通

### 7.2 【高】JPMS module-info
JDK 17 模块（aot/native/ai/rag/mcp）已经验证 --release 17 编译正常。
- **建议给 v0.10.0 之后的模块加 module-info.java**（core / aop / web / config / validation / exception / logging / scheduling / cache / metrics）
- 需 module path 重写测试隔离——`java.lang.module.ModuleFinder` 集成测试

### 7.3 【中】Sonatype OSS 发布
`publishToMavenLocal` 已实测 40 个构件全部发布。**进一步**：
- 配 `gradle.properties` 中的 `sonatypeUsername/sonatypePassword`
- 改 `build.gradle.kts` 的 PublishingExtension 配 `ossrh-staging-api`：发布到 `https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/`
- 等 staging 仓库中转后再 release 到 Maven Central

### 7.4 【中】grpc InProcess 完整集成
当前 grpc 走**单元测试**（3/3 全绿）。`InProcessServerBuilder` 在 `grpc-core` 而非 `grpc-api`，需加：
```kotlin
testImplementation("io.grpc:grpc-core:1.62.2")
testImplementation("io.grpc:grpc-stub:1.62.2")
testImplementation("io.grpc:grpc-netty-shaded:1.62.2")
```
**注意**：Maven Central 上 grpc-netty 体积较大（~6MB），CI 缓存可以解决。

### 7.5 【低】kafka/rmq/nats/mqtt 真实集成
参考 redis 模式（`DockerContainer.java` + `assumeTrue(dockerUp)`）补完 4 个传输：
- `apache/kafka:3.7.0`
- `rabbitmq:3-management`
- `nats:2.10-alpine`
- `eclipse-mosquitto:2.0`

### 7.6 【低】Javadoc + 站点
`./gradlew javadoc` 能产出 javadoc；可加 `asciidoctor` plugin 产出完整站点（受网络限流时先在 CI 跑）。

### 7.7 【低】Spring Boot 启动器
v1.0.0 后可做 `jvfault-spring-boot-starter`（自动配置、@SpringBootApplication → @Module 桥接）。

## 8. 接手即用的下一步

**如果只做一件事**（推荐顺序）：

1. **跑一次全量验证**确认接手时状态：
   ```bash
   cd /Users/Wang/Code/webfault-lib/jvfault-all/jvfault
   JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build
   ```
   期望 329 tasks BUILD SUCCESSFUL，245 tests 0 failed。
2. **配置 CI 镜像**（一次性工作，让后续 245 个测试在 CI 中跑全链路真实集成）：
   - 在 `.github/workflows/ci.yml` 加 `services:` 段启动 redis/kafka/rmq/nats/mqtt/grpc-stub
3. **推进 v1.0.0 → v1.1.0** 的下一个特性：建议**`jvfault-spring-boot-starter`**（Spring 桥接，最大化 Java 生态覆盖）。

## 9. 联系方式

- 框架 GitHub：https://github.com/rwx-robot/jvfault
- git 身份：johnnynode <johnnynode@gmail.com>
- 父 monorepo（workspace）：`/Users/Wang/Code/webfault-lib/jvfault-all/`

---

**本次会话归档完成。下一位 Agent 可从本工作区与本文档继续。**
