package com.jvfault.example.v010;

import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Inject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 问候服务 - 演示依赖注入和生命周期
 */
@Component
public class GreetingService {

    private final MessageProvider messageProvider;
    private boolean initialized = false;

    @Inject
    public GreetingService(MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
    }

    public String greet(String name) {
        return messageProvider.getMessage() + ", " + name + "!";
    }

    @PostConstruct
    public void init() {
        System.out.println("🔧 GreetingService initialized");
        initialized = true;
    }

    @PreDestroy
    public void destroy() {
        System.out.println("🗑️ GreetingService destroyed");
    }

    public boolean isInitialized() {
        return initialized;
    }
}