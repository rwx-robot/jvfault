package com.jvfault.example.v090;

import com.jvfault.core.annotation.Module;

/**
 * v0.9.0 示例模块 —— 仅用于触发编译期模块元数据生成。
 *
 * @since v0.9.0 (2023)
 */
@Module(providers = {GreetingService.class})
public class AppModule {
}

class GreetingService {

    String greet(String name) {
        return "hello " + name;
    }
}
