# 非闭环 / 未完成功能清单

> 盘点时间：2026-09-25（v1.0.13）
> 目的：区分「**有单元测试**」与「**真正跑得通的闭环**」。
> 一个模块有测试 ≠ 它能被外部工程真实用起来。本文只记录后者存在缺口的部分。

---

## 一、结论速览

| 类别 | 数量 | 说明 |
|------|:----:|------|
| 有单元测试的模块 | 39 / 40 | `test` 模块**无测试** |
| **有可运行示例**的模块 | **约 12 / 40** | 仅 5 个示例，覆盖严重不足 |
| 只有单元测试、无示例闭环 | 约 28 | 见第三节 |
| 单类薄实现模块 | 6 | 见第四节 |
| 明确未完成的代码点 | 2 | 见第五节 |

---

## 二、已验证为真闭环的部分 ✅

**外部消费闭环（2026-09-25 实测通过）**
```
./gradlew publishToMavenLocal
  → 外部工程只依赖 m2 中已发布的 core-1.0.13.jar
  → 编译 OK
  → 容器启动 → bean 注入 → 取到实例
  → 输出：CONSUMER_OK: hello-from-m2 (beans=1) / CLOSED_LOOP_OK
```
即「发布 → 被外部依赖 → 启动 → 注入 → 拿到 bean」整条链路真实可用。

**可运行示例（5 个）**

| 示例 | 覆盖模块 |
|------|----------|
| v0.8.0 | `virtualthreads` |
| v0.9.0 | `plugin` / `apt` / `aot` |
| v0.10.0 | `logging` / `native` |
| v0.11.0 | `ai` / `rag`（示例内 `ChatModel` 是 stub） |
| v1.0.0 | `security` / `compliance` / `migration` / `ops` |

**集成测试闭环**
- `web` → `platform-servlet` → `security`：`tests` 模块的 Jetty + JWT 端到端套件
- `transport-*`（7 个）：CI 上连真实 broker（redis / kafka / rabbitmq / nats / mosquitto）+ gRPC 回环

---

## 三、只有单元测试、**没有可运行示例**的模块（约 28 个）

> 这些模块有测试覆盖，但**没有任何示例把它们跑起来**，
> 因此「能否在真实应用里组合使用」未被证明。

`core`（IoC 内核，虽被所有示例间接用到，但自身无独立示例）
`aop` · `config` · `validation` · `exception` · `scheduling` · `cache` · `tracing` ·
`metrics` · `microservices` · `web` · `websocket` · `sse` · `openapi` · `graphql` ·
`platform-servlet` · `platform-reactive` · `transport-tcp` · `transport-grpc` ·
`transport-kafka` · `transport-redis` · `transport-nats` · `transport-rmq` ·
`transport-mqtt` · `mcp` · `test` · `spring-boot-starter`

**其中最值得注意：`web`**
它是框架的门面能力（`@Controller` 路由 / `WebApplication` / Guard 管道），
但**没有任何可运行示例**（原本应由 v0.1.0–v0.7.0 的示例承担，那些已在 v1.0.10 删除）。
目前仅靠 `tests` 模块的 Jetty 端到端测试证明可用 —— 属于「测试级闭环」，不是「示例级闭环」。

**`spring-boot-starter`**
v1.0.11 新增，只有 `ApplicationContextRunner` 单元测试，
**没有真实 Spring Boot 应用的端到端示例**。

**`mcp`**
未在任何一个示例中出现。

---

## 四、单类薄实现模块（6 个）

以下模块每个**只有 `module-info.java` + 1 个实现类**，功能面很窄，
且都属于「Java 8 冒烟未能验证」的 6 个模块（因依赖 Spring / WebFlux 等三方库）：

| 模块 | 唯一实现类 | 备注 |
|------|-----------|------|
| `aot` | `ReflectConfigGenerator` | 生成 GraalVM 反射配置 |
| `apt` | `ModuleMetadataProcessor` | 编译期注解处理器 |
| `graphql` | `GraphQLEndpoint` | 单个端点类，无 schema 装配示例 |
| `native` | `NativeRuntimeHints` | 仅提示，**未真正执行 native-image 构建** |
| `openapi` | `OpenApiGenerator` | 文档生成 |
| `platform-reactive` | `ReactiveResultHandler` | Reactor 渲染桥接 |

> `native` 尤其值得注意：v0.10.0 示例输出为 `native 模式: false`，
> 说明它只是**模拟/生成配置**，从未真正用 GraalVM 构建出原生镜像。

---

## 五、明确未完成的代码点（2 处）

### 1. `ThreadPoolTaskScheduler` 返回的 Future 不能等待

`scheduling/src/main/java/com/jvfault/scheduling/ThreadPoolTaskScheduler.java:162-163`

```java
@Override public Object get()                       { throw new UnsupportedOperationException(); }
@Override public Object get(long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
```

调度返回的 Future **不支持 `get()`**，调用方无法等待任务结果或做超时控制。
这是明确的实现缺口（不是设计取舍）。

### 2. `test` 模块没有测试

`test/src/test/java/` 是**空目录**（0 个 `.java`）。
该模块对外提供 JUnit 5 扩展（`@TestModule` + `@Autowired`），自身却零测试。
（已顺带修掉一个由此产生的错误宣称：README 曾写「全部 40 个均含测试」，
实际含测试的是 **39** 个 —— 校验逻辑此前只判目录是否存在。）

---

## 六、其他非代码类缺口（详见 `TODO.md`）

| 缺口 | 影响 |
|------|------|
| `distribution` BOM 模块从未落地 | 使用者无法一行锁定 40 个模块版本 |
| 未发布到 Maven Central（缺 Sonatype 凭据） | 外部只能用 clone 或本地 m2 |
| 6 个模块 Java 8 未严格验证 | 「Java 8 基线」承诺在这 6 个上未被证明 |
| 命名不一致 | 本地 jar 名 `jvfault-core-1.0.13.jar`，发布坐标却是 `com.jvfault:core`（artifactId = 项目名，不含 `jvfault-` 前缀） |

---

## 七、建议的补齐顺序

1. **`web` 可运行示例** —— 门面能力没有示例，是最大的闭环缺口
2. **`ThreadPoolTaskScheduler.get()`** —— 明确的实现缺口，应实现或改为返回可等待的句柄
3. **`test` 模块补测试** —— 提供测试基础设施却自身零测试，说服力不足
4. **`native` 真正跑一次 native-image** —— 否则该模块只是"配置生成器"
5. 其余按 `TODO.md` 推进
