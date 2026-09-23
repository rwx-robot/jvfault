/**
 * jvfault cache —— @Cacheable/@CacheEvict、多级缓存（Caffeine）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.cache {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;
    requires com.github.benmanes.caffeine;

    exports com.jvfault.cache;
    exports com.jvfault.cache.annotation;
    exports com.jvfault.cache.cache;
}