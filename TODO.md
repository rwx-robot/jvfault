# jvfault TODO — 剩余工作清单

> 最后更新：2026-09-25（对应 **v1.0.13**）
> 状态口径：✅ 已完成 / ⏸ 待 Ny 决策或外部凭据 / 🔧 技术债（可自主推进）

---

## 一、待办总览

| # | 优先级 | 事项 | 状态 | 阻塞点 |
|---|:------:|------|:----:|--------|
| 1 | **P1** | Sonatype OSS 发布（Maven Central） | ⏸ | 缺凭据 |
| 2 | **P1** | spring-boot-starter：反向注入（Spring→jvfault） | ⏸ | 待 Ny 决策 |
| 3 | **P1** | spring-boot-starter：示例工程 | ⏸ | 待 Ny 决策 |
| 4 | **P2** | `distribution` BOM 模块缺失 | 🔧 | 无（可自主做） |
| 5 | **P2** | Java 8 冒烟未能覆盖 6 个模块 | 🔧 | 无（可自主做） |
| 6 | **P2** | Javadoc 站点 | 🔧 | 无 |
| 7 | **P2** | 「18 个传输集成测试」仍未被机检 | 🔧 | 部分难静态校验 |
| 8 | — | spring-boot-starter 无 `module-info` | ✅ 刻意 | 见下方说明 |

---

## 二、明细

### 1️⃣ Sonatype OSS 发布（P1 · ⏸ 缺凭据）

**现状**：`./gradlew publishToMavenLocal` 已验证可用；`maven-publish` 已为全部发布模块配置好 `pom`（名称/描述/Apache-2.0）。
**阻塞**：需要 `sonatypeUsername` / `sonatypePassword` 与 `ossrh-staging-api`。
**验收**：
1. 配置凭据后 `./gradlew publish` 能推到 OSSRH staging
2. staging 仓库 close → release 后，Maven Central 上能搜到 `com.jvfault:core:1.0.13`
**备注**：这是「框架能被外部真正使用」的最后一步。在此之前使用者只能 clone 源码或装到本地 m2。

---

### 2️⃣ spring-boot-starter 反向注入（P1 · ⏸ 待 Ny 决策）

**现状**：已实现 **jvfault → Spring** 单向暴露（jvfault 的 bean 注册为 Spring 单例）。
**待定**：反向（把 Spring bean 塞进 jvfault 的 `BeanRegistry`）。
**为什么当初没做**：会把两个容器的生命周期与依赖解析纠缠在一起，循环依赖极难诊断。
**若要做**，建议限定为显式开关（如 `jvfault.import-spring-beans=true`），且只导入使用者显式标注的 bean，避免全量导入引发冲突。
**验收**：能在一个 Spring Boot 应用里把 Spring 管理的 bean 注入 jvfault 组件，且有测试覆盖生命周期不泄漏。

---

### 3️⃣ spring-boot-starter 示例工程（P1 · ⏸ 待 Ny 决策）

**现状**：有 2 个单元测试（`ApplicationContextRunner`）覆盖激活与未激活。
**缺口**：没有端到端可运行的 Spring Boot 示例（真实 `@SpringBootApplication` + controller）。
**成本**：需引入 `spring-boot-starter-web`，会新增一个模块并再次改动机检数字。
**验收**：`./gradlew :examples:spring-boot:run` 能起服务并返回一个由 jvfault 组件处理的响应。

---

### 4️⃣ `distribution` BOM 模块缺失（P2 · 🔧 可自主做）

**问题**：根 `build.gradle.kts` 里已有防御分支 `if (name == "distribution" ...) return@subprojects`
（注释写「BOM 配置在 `distribution/build.gradle.kts`」），但：

- `distribution/` 目录**不存在**
- `settings.gradle.kts` **没有** `include(":distribution")`

即 BOM 是**规划过但从未落地**的缺口。
**影响**：使用者无法用一行 BOM 锁定全部 40 个模块的版本，必须逐个指定。
**验收**：
1. 新增 `distribution/build.gradle.kts`（`java-platform`，constraints 覆盖全部发布模块）
2. `settings.gradle.kts` 加入 include
3. `publishToMavenLocal` 产出 `jvfault-distribution` BOM pom

---

### 5️⃣ Java 8 冒烟未覆盖 6 个模块（P2 · 🔧 可自主做）

**问题**：`platform-reactive` / `openapi` / `native` / `aot` / `graphql` / `sse` 这 6 个模块依赖
Spring、WebFlux 等三方库；冒烟脚本 classpath 只有 jvfault 自身 + slf4j，
因此它们的类会因依赖缺失加载失败。脚本已把这类错误排除在「Java 8 不兼容」之外，
但**意味着这 6 个实际未被严格验证**。
**验收**：把三方依赖加入冒烟 classpath，让这 6 个也被真 Java 8 验证；
或在 README 中明确标注「不支持 Java 8 运行时」（目前只说未验证，口径偏软）。

---

### 6️⃣ Javadoc 站点（P2 · 🔧）

**现状**：`./gradlew javadoc` 已可产出（全局配置了 `-Xdoclint:none -quiet`）。
**缺口**：没有聚合站点与发布渠道。
**验收**：用 asciidoctor 生成聚合站点，挂 GitHub Pages。

---

### 7️⃣ 「18 个传输集成测试」仍未被机检（P2 · 🔧 部分难校验）

**问题**：README 写「18 个传输集成测试在 CI 上真实执行」，这个数**没有**机检。
难处：它依赖 CI 上 broker 是否就绪 —— 本地无 broker 时这些用例是 skip 而非消失，
所以总数不变、无法用总数校验；需要单独统计「传输集成测试类的用例数」。
**验收**：加一条统计传输集成用例数的机检，或把该表述改为不写死数字。

---

### 8️⃣ spring-boot-starter 无 `module-info`（✅ 刻意，非缺陷）

Spring Boot 的自动配置并非 JPMS 友好；本模块按 classpath 适配器发布，
与 `tests` 一样不进 MR-JAR。等 Spring 侧模块名稳定后再评估。

---

## 三、已关闭（本轮完成，不再列入待办）

| 事项 | 完成于 |
|------|--------|
| ponytail 审计 5 项（1 已修、2 已删、2 论证后不采纳） | v1.0.10 |
| 删除 7 个历史示例模块（12 → 5） | v1.0.10 |
| 删除 `MeterRegistry` 投机性接口 | v1.0.10 |
| JDK 21 引用修正（绝对路径，写明向下兼容） | v1.0.10 |
| 测试总数漂移 → CI 脚本 `verify-readme-test-count.sh` | v1.0.10 |
| `spring-boot-starter`（模块化/插件化） | v1.0.11 |
| 版本 badge ↔ `gradle.properties` 机检 | v1.0.12 |
| JPMS 回归例数机检（并修正 6 → 8） | v1.0.12 |
| CI broker 数 / CI JDK 覆盖机检 | v1.0.13 |
| GitHub About 描述 | Ny 已手动完成 |
