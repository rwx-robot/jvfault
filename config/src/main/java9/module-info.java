/**
 * jvfault config —— Environment、YAML/Properties/JSON、@Value/@ConfigurationProperties。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.config {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;
    requires org.yaml.snakeyaml;
    requires com.fasterxml.jackson.databind;

    exports com.jvfault.config;
    exports com.jvfault.config.annotation;
}