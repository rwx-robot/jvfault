package com.jvfault.core;

import com.jvfault.core.annotation.Module;
import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.container.BeanRegistry;
import com.jvfault.core.module.ModuleContainer;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ModuleContainer 核心功能测试
 * 
 * @since v0.1.0 (2015)
 */
@DisplayName("Core ModuleContainer Tests")
class ModuleContainerTest {

    private ModuleContainer container;

    @BeforeEach
    void setUp() {
        container = JvfaultApplication.createContainer(CoreTestModule.class);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.destroy();
        }
    }

    @Test
    @DisplayName("Should create container and register beans")
    void testContainerCreation() {
        BeanRegistry registry = container.getBeanRegistry();
        
        assertNotNull(registry);
        assertTrue(registry.containsBean(TestServiceA.class));
        assertTrue(registry.containsBean(TestServiceB.class));
        assertTrue(registry.containsBean(PrototypeService.class));
    }

    @Test
    @DisplayName("Should resolve singleton beans")
    void testSingletonResolution() {
        BeanRegistry registry = container.getBeanRegistry();
        
        TestServiceA bean1 = registry.getBean(TestServiceA.class);
        TestServiceA bean2 = registry.getBean(TestServiceA.class);
        
        assertSame(bean1, bean2, "Singleton should return same instance");
        assertTrue(bean1.isInitialized(), "@PostConstruct should be called");
    }

    @Test
    @DisplayName("Should inject dependencies via constructor")
    void testConstructorInjection() {
        BeanRegistry registry = container.getBeanRegistry();
        
        TestServiceB serviceB = registry.getBean(TestServiceB.class);
        
        assertNotNull(serviceB);
        assertEquals("B -> Hello from ServiceA", serviceB.combined());
    }

    @Test
    @DisplayName("Should create new instance for prototype scope")
    void testPrototypeScope() {
        BeanRegistry registry = container.getBeanRegistry();
        
        PrototypeService p1 = registry.getBean(PrototypeService.class);
        PrototypeService p2 = registry.getBean(PrototypeService.class);
        
        assertNotSame(p1, p2, "Prototype should return new instance each time");
        assertNotEquals(p1.getId(), p2.getId());
    }

    @Test
    @DisplayName("Should support getBeansOfType")
    void testGetBeansOfType() {
        BeanRegistry registry = container.getBeanRegistry();
        
        Map<String, TestServiceA> beans = registry.getBeansOfType(TestServiceA.class);
        
        assertEquals(1, beans.size());
        assertTrue(beans.containsKey("testServiceA"));
    }

    @Test
    @DisplayName("Should call @PreDestroy on shutdown")
    void testPreDestroyCallback() {
        BeanRegistry registry = container.getBeanRegistry();
        TestServiceA serviceA = registry.getBean(TestServiceA.class);
        
        container.destroy();
        
        assertTrue(serviceA.isDestroyed(), "@PreDestroy should be called on destroy");
    }

    @Test
    @DisplayName("Should handle module imports")
    void testModuleImports() {
        // 创建一个导入 CoreTestModule 的新模块
        @Module(imports = CoreTestModule.class)
        class ImportingModule {}
        
        ModuleContainer importContainer = JvfaultApplication.createContainer(ImportingModule.class);
        
        try {
            assertTrue(importContainer.getBeanRegistry().containsBean(TestServiceA.class));
        } finally {
            importContainer.destroy();
        }
    }
}