/**
 * jvfault migration —— Spring → jvfault 重写规则表、源码扫描器、Markdown 迁移报告。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 17）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.migration {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;

    exports com.jvfault.migration;
}