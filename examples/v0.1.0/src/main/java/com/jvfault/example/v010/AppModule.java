package com.jvfault.example.v010;

import com.jvfault.core.annotation.Module;

/**
 * v0.1.0 示例应用模块
 * 演示核心 IoC 容器功能
 */
@Module(providers = {GreetingService.class, HelloMessageProvider.class})
public class AppModule {
}