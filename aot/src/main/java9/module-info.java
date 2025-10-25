/**
 * jvfault aot —— GraalVM 反射配置生成与 native 提示。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.aot {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;

    exports com.jvfault.aot;
}