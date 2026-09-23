/**
 * jvfault openapi —— OpenAPI 3.0 文档生成。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.openapi {

    requires transitive com.jvfault.web;
    requires transitive org.slf4j;
    requires org.yaml.snakeyaml;
    requires com.fasterxml.jackson.databind;

    exports com.jvfault.openapi;
}