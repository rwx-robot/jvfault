/**
 * jvfault apt —— 编译期 @Module 元数据生成（META-INF/jvfault/modules.txt）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。注解处理器仅以 {@code compileOnly}
 * 依赖 core，本模块在运行时不需要 core。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.apt {

    exports com.jvfault.apt;
}