package com.jvfault.core.container;

/**
 * 当容器中不存在请求的 Bean 时抛出。
 * 对应 Spring: NoSuchBeanDefinitionException
 *
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
public class NoSuchBeanDefinitionException extends RuntimeException {

    public NoSuchBeanDefinitionException(String message) {
        super(message);
    }

    public NoSuchBeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
