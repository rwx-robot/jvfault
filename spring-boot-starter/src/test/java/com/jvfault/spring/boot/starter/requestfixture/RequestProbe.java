package com.jvfault.spring.boot.starter.requestfixture;

import com.jvfault.core.annotation.Component;

/**
 * jvfault 的 REQUEST 作用域组件：用来验证「暴露到 Spring 后作用域是否还能保持」。
 */
@Component(scope = Component.Scope.REQUEST)
public class RequestProbe {

    public int mark;
}
