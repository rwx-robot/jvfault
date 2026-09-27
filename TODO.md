# jvfault TODO — 剩余工作清单

> 最后更新：2026-09-27（对应 **v1.0.13 · 引擎现代化**）
> 状态口径：✅ 已完成 / ⏸ 待 Ny 决策或外部凭据 / 🔧 技术债（可自主推进）

---

## 一、待办总览

| # | 优先级 | 事项 | 状态 | 阻塞点 |
|---|:------:|------|:----:|--------|
| 1 | **P1** | Sonatype OSS 发布（Maven Central） | ⏸ | 缺凭据 |
| 2 | **P1** | spring-boot-starter：反向注入（Spring→jvfault） | ⏸ | 待 Ny 决策 |
| 3 | **P1** | spring-boot-starter：示例工程 | ⏸ | 待 Ny 决策 |
| 4 | **P2** | `distribution` BOM 模块缺失 | ✅ | 已落地（commit cfee7dd） |
| 5 | **P2** | Java 8 冒烟未能覆盖 6 个模块 | ✅ | 已做字节码级验证（见下） |
| 6 | **P2** | Javadoc 站点 | ✅ | 聚合站点 + Pages 部署（commit c1337ff） |
| 7 | **P2** | 「18 个传输集成测试」仍未被机检 | ✅ | 已加 scripts/verify-transport-it-count.sh（commit f70c39b） |
| 8 | — | spring-boot-starter 无 `module-info` | ✅ 刻意 | 见下方说明 |

---

## 二、明细

### 1️⃣ Sonatype OSS 发布（P1 · ⏸ 缺凭据）

**现状**：`./gradlew publishToMavenLocal` 已验证可用；`maven-publish` 已为全部发布模块配置好 `pom`（名称/描述/Apache-2.0）。
**阻塞 / 待 Ny 提供的信息（填好后我负责补 `signing` + `maven-publish` 并跑 `./gradlew publish`）：**
- [ ] **Sonatype 账号**：`sonatypeUsername` / `sonatypePassword`（旧 OSSRH `issues.sonatype.org` 或新 `central.sonatype.com` 账号）
- [ ] **groupId 批准**：`com.jvfault` 这个命名空间需在 OSSRH 首次发布前申请并获批
- [ ] **GPG 签名**：Maven Central 强制签名。需 GPG 私钥 + 公钥传到 keyserver；提供 `signing.keyId` / `signing.password` / `signing.secretKeyRingFile`（或改用 `signingInMemoryPgpKeys` 内联）
- [ ] **staging 目标**：旧 OSSRH 用 `https://oss.sonatype.org/service/local/staging/deploy/maven2/`（配 `ossrh-staging-api`）；新 Central Portal 用 Portal API + snapshot 仓库
- [ ] **POM 补全**：SCM 地址、开发者信息、CI 自动发布开关（当前仓库缺，需补到 `gradle.properties` 或 `build.gradle.kts`）
- [ ] **本地已就绪**：`./gradlew publishToMavenLocal` 通过；`maven-publish` 已为全部发布模块配好 pom（名称/描述/Apache-2.0）
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

### 9️⃣ 引擎现代化：对齐主流 Java 框架 SDK（P2 · ✅ 本轮完成）

用户要求「把引擎改过来，别人用什么 SDK 你给我用什么 SDK，不要再用 Java 8」——本轮落地：

- **根基线 Java 8 → 17**：根 `build.gradle.kts` 的 `options.release` 由 `8` 改为 `17`，
  对齐 **Spring Boot 3 / Jakarta EE 10** 的 floor；虚拟线程模块仍按 21 编译。
  全部 40 模块 + 5 示例 `./gradlew build -x test` 通过。
- **Caffeine 2.9.3 → 3.1.8**：`cache/build.gradle.kts`，与 Spring Boot 3 托管版本一致
  （要求 Java 11+，Java 17 基线满足）；`CaffeineCache` 用法 2.x→3.x 完全兼容。
- **validation 引擎换血**：自研的 JSR-380 子集反射引擎 →
  **Jakarta Validation 3.0.2 + Hibernate Validator 8.0.1 + Jakarta EL（expressly 5.0.0）**，
  即 Spring Boot 3 / Quarkus / Jakarta EE 同一套校验栈。删除自研 `constraint/*` 注解与反射引擎，
  约束注解改用 `jakarta.validation.constraints.*`；保留 `Validator` / `ValidationEngine` /
  `ConstraintViolation` / `ValidationException` 门面 API（已重写测试，12 个用例全绿）。
- **README 去 Java 8 口径**：去掉「默认基线 JDK 8」起头，新增「对齐主流 SDK」小节；
  移除已无用的 `scripts/java8-runtime-smoke`（基线不再是 8）。

> 说明：Hibernate Validator 默认消息会随 JVM locale 本地化（如中文「不能为null」），
> 框架测试已固定 `Locale.ENGLISH` 以保证断言稳定；自定义消息建议用命名占位符
> `{min}`/`{max}` 等，位置占位符 `{0}`（属性名）是否被插值取决于校验 Provider 配置。

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
| `distribution` BOM 模块（40 模块版本对齐） | v1.0.13+（commit cfee7dd） |
| 传输集成测试数量机检（`scripts/verify-transport-it-count.sh`） | v1.0.13+（commit f70c39b） |
| Java 8 冒烟升级为逐模块字节码基线验证（覆盖原「6 未验证」模块） | v1.0.13+ |
| 聚合 Javadoc 站点任务 + GitHub Pages 自动部署（pages.yml） | v1.0.13+（commit c1337ff） |
