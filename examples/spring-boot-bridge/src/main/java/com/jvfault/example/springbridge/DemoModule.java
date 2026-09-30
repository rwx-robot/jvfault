package com.jvfault.example.springbridge;

import com.jvfault.core.annotation.Module;

/**
 * jvfault 根模块：{@code jvfault.base-packages} 扫描范围内唯一的 {@code @Module}，
 * 可被自动推断，无需显式配置 {@code jvfault.root-module}。
 *
 * @since v1.0.14 (2026)
 */
@Module(providers = {Greeter.class})
public class DemoModule {
}
