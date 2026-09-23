/**
 * jvfault validation —— JSR-380 风格约束校验（级联 + 自定义校验器 SPI）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.validation {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;

    exports com.jvfault.validation;
    exports com.jvfault.validation.constraint;
}