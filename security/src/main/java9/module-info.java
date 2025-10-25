/**
 * jvfault security —— JWT HS256、PBKDF2 口令哈希、JwtGuard。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.security {

    requires transitive com.jvfault.core;
    requires transitive com.jvfault.web;
    requires transitive org.slf4j;

    exports com.jvfault.security.crypto;
    exports com.jvfault.security.guard;
    exports com.jvfault.security.jwt;
}