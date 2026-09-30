package com.jvfault.spring.boot.starter.requestfixture;

import com.jvfault.core.annotation.Module;

/**
 * REQUEST 作用域场景的根模块：base-packages 下<b>唯一</b>的 {@code @Module}。
 */
@Module(providers = {RequestProbe.class})
public class RequestModule {
}
