/**
 * jvfault validation —— 基于 Jakarta Validation + Hibernate Validator 的 Bean 校验。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 17）。
 *
 * @since v1.1.0
 */
module com.jvfault.validation {

    requires transitive com.jvfault.core;
    requires transitive org.slf4j;
    requires jakarta.validation;
    requires org.hibernate.validator;

    exports com.jvfault.validation;
}
