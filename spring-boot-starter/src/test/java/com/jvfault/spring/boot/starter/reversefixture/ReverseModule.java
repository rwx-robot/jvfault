package com.jvfault.spring.boot.starter.reversefixture;

import com.jvfault.core.annotation.Module;
import com.jvfault.spring.boot.starter.reversefixture.TimeAwareGreeter;

/**
 * 反向注入测试用的根模块：位于与 fixture 平级的独立包，避免被
 * base-packages=...fixture 的递归扫描误命中（否则会出现 2 个 @Module）。
 * 仅声明 {@link TimeAwareGreeter} 一个 provider。
 */
@Module(providers = {TimeAwareGreeter.class})
public class ReverseModule {
}
