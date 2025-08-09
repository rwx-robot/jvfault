package com.jvfault.core.container;

/**
 * 当容器中存在多个匹配类型的 Bean 且无法消歧时抛出。
 * 对应 Spring: NoUniqueBeanDefinitionException
 *
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
public class NoUniqueBeanDefinitionException extends RuntimeException {

    public NoUniqueBeanDefinitionException(String message) {
        super(message);
    }

    public NoUniqueBeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
