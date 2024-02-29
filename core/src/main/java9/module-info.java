/**
 * jvfault core —— IoC 容器与模块系统。
 *
 * <p>本描述符位于多版本 JAR 的 {@code META-INF/versions/9/} 中，
 * 仅以 {@code --release 9} 编译；模块主代码仍以 {@code --release 8} 编译，
 * 因此本构件同时兼容 Java 8（走 Automatic-Module-Name）与 Java 9+（走本描述符）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.core {

    // 第三方（自动模块）
    requires jakarta.inject;
    requires jakarta.annotation;
    requires io.github.classgraph;
    requires org.slf4j;

    // 框架反射（扫描 / IoC / AOP）以用户类为对象，故自身包全部导出即可
    exports com.jvfault.core.annotation;
    exports com.jvfault.core.bootstrap;
    exports com.jvfault.core.container;
    exports com.jvfault.core.module;
    exports com.jvfault.core.scanner;
    exports com.jvfault.core.spi;
    exports com.jvfault.core.util;
}
