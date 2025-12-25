/**
 * jvfault test —— JUnit 5 扩展 (@TestModule + @Autowired)。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.test {

    requires transitive com.jvfault.core;

    exports com.jvfault.test;
}