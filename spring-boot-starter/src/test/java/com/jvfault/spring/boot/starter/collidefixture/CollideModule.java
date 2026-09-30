package com.jvfault.spring.boot.starter.collidefixture;

import com.jvfault.core.annotation.Module;

/**
 * 撞名场景的根模块：base-packages 下<b>唯一</b>的 {@code @Module}（放独立包是刻意的 ——
 * jvfault 会递归扫描 base-packages，多一个 @Module 就无法推断根模块）。
 */
@Module(providers = {Greeter.class})
public class CollideModule {
}
