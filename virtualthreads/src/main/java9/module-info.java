/**
 * jvfault virtualthreads —— 虚拟线程执行器与结构化并发。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.virtualthreads {

    requires transitive org.slf4j;

    exports com.jvfault.virtualthreads;
}