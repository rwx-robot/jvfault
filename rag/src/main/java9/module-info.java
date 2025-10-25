/**
 * jvfault rag —— 向量检索 / RAG pipeline。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 17）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.rag {

    requires transitive com.jvfault.ai;
    requires transitive org.slf4j;

    exports com.jvfault.rag;
}