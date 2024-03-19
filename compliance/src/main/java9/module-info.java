/**
 * jvfault compliance —— 审计脱敏（只追加 JSONL AuditTrail + DataMasker）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 17）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.compliance {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;
    requires com.fasterxml.jackson.databind;

    exports com.jvfault.compliance;
}