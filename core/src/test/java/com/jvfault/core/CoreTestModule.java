package com.jvfault.core;

import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Inject;
import com.jvfault.core.annotation.Module;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 核心模块测试用例
 */

// ============ 测试组件 ============

@Component
class TestServiceA {
    private boolean initialized = false;
    private boolean destroyed = false;

    public String sayHello() {
        return "Hello from ServiceA";
    }

    @PostConstruct
    void init() {
        initialized = true;
    }

    @PreDestroy
    void destroy() {
        destroyed = true;
    }

    boolean isInitialized() { return initialized; }
    boolean isDestroyed() { return destroyed; }
}

@Component
class TestServiceB {
    private final TestServiceA serviceA;

    @Inject
    TestServiceB(TestServiceA serviceA) {
        this.serviceA = serviceA;
    }

    public String combined() {
        return "B -> " + serviceA.sayHello();
    }
}

@Component(scope = Component.Scope.PROTOTYPE)
class PrototypeService {
    private final int id = hashCode();
    
    public int getId() { return id; }
}

// ============ 测试模块 ============

@Module(providers = {TestServiceA.class, TestServiceB.class, PrototypeService.class})
public class CoreTestModule {
    // 模块类本身可以为空，仅作为 @Module 承载
}