package com.jvfault.spring.boot.starter.fixture;

import com.jvfault.core.annotation.Module;

/** 测试用根模块（basePackages 下唯一一个 @Module，可被自动推断）。 */
@Module(providers = {GreetingService.class})
public class TestAppModule {
}
