# Ponytail 代码审计报告

> 审计时间：2026-09-23
> 审计范围：`/jvfault` 下所有模块（不含 examples/）
> 审计原则：仅删除过工程化（over-engineering）代码；正确性、安全、性能问题不在本审计范围内

---

## 结论总览

| 标签 | 发现 | 处置 | 预计减少 |
|------|------|------|----------|
| `delete` | `ExceptionHandlerRegistry.distance()` — 永远返回 0，死代码 | ✅ 已处理（commit `b3c2d91`） | ~10 行 |
| `delete` | 历史版本示例模块 v0.1.0 ~ v0.7.0（保留 v0.8.0+ 即可） | 待 Ny 决定 | ~730 行 |
| `delete` | `MeterRegistry` 接口（唯一实现 DefaultMeterRegistry，投机性桥接设计） | 待 Ny 决定 | ~28 行 |
| `shrink` | `LogEntry.Builder`（110 行 builder，数据类仅 8 字段） | 待 Ny 决定 | ~70 行 |
| `stdlib` | `DefaultMeterRegistry.toTags()` 手写循环（可用 stream/foreach） | 待 Ny 决定 | ~5 行 |

**最大可删：~830 行**（主要是 11 个历史示例模块）

---

## 已处理 ✅

### `delete` `ExceptionHandlerRegistry.distance()`（2026-09-23 已删）

**文件**：`exception/src/main/java/com/jvfault/exception/ExceptionHandlerRegistry.java`

**问题**：方法永远返回 0，从未被正确实现。

```java
// 旧代码（已删）
private int distance(Class<? extends Throwable> type) {
    return 0; // ← 永远返回 0，从未计算继承距离
}

// 调用处（已删）
.add(new Entry(handlerBean, method, distance(type))); // ← distance(type) 永远传 0

// 解析时（已删）
int effective = d + entry.declaredDistance; // declaredDistance 永远是 0
```

注册时距离永远为 0（自身类型），`declaredDistance` 字段和 `Entry` 的双构造器都是死代码。

**改后**：删除了 `distance()` 方法、`declaredDistance` 字段、`Entry(Object, Method, int, int)` 重载构造器；Entry 简化为单字段 `effectiveDistance`。

**验证**：`./gradlew :exception:test` → 10 tests 0 failures ✅

---

## 待 Ny 决定

### `delete` 历史版本示例模块（v0.1.0 ~ v0.7.0）

**文件**：`examples/v0.{1,2,3,4,5,6,7}.0/`

**问题**：11 个历史版本示例模块（v0.1.0 ~ v0.7.0），每个 ~3-18MB，每次 CI 全量构建都跑一遍。当前 `examples/v0.8.0`（208KB）和 `v0.9.0` ~ `v1.0.0` 规模较大（~3-18MB），建议也斟酌。

| 模块 | 大小 | 建议 |
|------|------|------|
| `v0.1.0` ~ `v0.7.0`（共 7 个） | 3-15MB | **删除**（最早的历史版本，展示价值最低） |
| `v0.8.0` | 208KB | **保留**（版本最小，可作轻量参考） |
| `v0.9.0` ~ `v0.11.0` | 3-18MB | **Ny 决定**（版本跨度大） |
| `v1.0.0` | 16MB | **保留**（最新，框架完整演示） |

**风险**：删除后现有用户若 clone 了这些模块，本地仍存在但不在 Gradle 里了（`git rm` 后不影响已有 clone）。

**操作**：
```bash
git rm -r examples/v0.1.0 examples/v0.2.0 examples/v0.3.0 \
        examples/v0.4.0 examples/v0.5.0 examples/v0.6.0 examples/v0.7.0
git commit -m "chore: 删除历史版本示例 v0.1.0~v0.7.0（ponytail 审计）"
```

---

### `delete` `MeterRegistry` 接口

**文件**：`metrics/src/main/java/com/jvfault/metrics/MeterRegistry.java`

**问题**：接口只有唯一实现 `DefaultMeterRegistry`，所有调用方都直接引用实现类。注释写"保留桥接可能"——这是投机性设计（YAGNI）。

```java
public interface MeterRegistry {          // ~28 行，仅一个实现
    void record(String name, long value);
    MetricsSnapshot snapshot();
}

public class DefaultMeterRegistry implements MeterRegistry {
    // 所有调用方：
    DefaultMeterRegistry registry = new DefaultMeterRegistry(); // 直接引用实现
}
```

**两个选择**：
- **激进**：删除接口，把 `implements MeterRegistry` 从 `DefaultMeterRegistry` 去掉。`DefaultMeterRegistry` 变成普通类。
- **保守**：保留接口（框架演进后可能有其他实现），不做任何改动。

---

### `shrink` `LogEntry.Builder`（110 行 builder，数据类仅 8 字段）

**文件**：`logging/src/main/java/com/jvfault/logging/LogEntry.java`

**问题**：110 行的 builder 类处理 8 个字段，每个 setter 几乎都是一行。可以用更简洁的方式替代。

**唯一调用方**：`StructuredLogger.java:174`：`LogEntry.builder().timestamp(...).level(...).build()`

**选项**：
- **record（Java 14+）**：8 个字段直接用 `record LogEntry(...) {}`，去掉 builder → ~30 行
- **保留 builder**：如果 API 链式调用是刻意设计，保留

---

### `stdlib` `DefaultMeterRegistry.toTags()`

**文件**：`metrics/src/main/java/com/jvfault/metrics/DefaultMeterRegistry.java:94-100`

**问题**：手写 `String[]` 转 `Map<String, String>` 循环，std stream/foreach 更简洁：

```java
// 旧代码（5 行）
Map<String, String> tags = new LinkedHashMap<>();
for (int i = 0; i < keys.length; i++) {
    tags.put(keys[i], values[i]);
}

// shrink：直接用 foreach
Map<String, String> tags = new LinkedHashMap<>();
for (int i = 0; i < keys.length; i++) tags.put(keys[i], values[i]);
```

改动极小（1 行），风险低，可接受。

---

## Net Summary

```
已处理（commit b3c2d91）：ExceptionHandlerRegistry.distance() 死代码 ~10 行
最大可删：11 个历史版本示例 ~730 行（需 Ny 同意）
可选优化：MeterRegistry 接口 ~28 行 + LogEntry.Builder ~70 行 + toTags ~5 行
─────────────────────────────────────────────────
最大可删总计：~830 行
```

---

## 参考：审计手法

本审计使用 ponytail 原则（[jvfault/ponytail](https://github.com/rwx-robot/ponytail)）扫描以下模式：

- **stdlib**：手写标准库已提供的功能
- **yagni**：投机性抽象（保留"未来可能"的接口/配置）
- **dead code**：从不调用或永远返回固定值的方法
- **mass**：重复代码或模块（历史版本示例属于此类）
- **shrink**：同一逻辑可以更少行写出
