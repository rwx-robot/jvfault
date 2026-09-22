package com.jvfault.example.v010;

import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.module.ModuleContainer;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * v0.1.0 示例应用集成测试
 */
class ApplicationIntegrationTest {

    private ModuleContainer container;

    @BeforeEach
    void setUp() {
        container = JvfaultApplication.createContainer(AppModule.class);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.destroy();
        }
    }

    @Test
    void testApplicationStarts() {
        assertNotNull(container);
        assertEquals(1, container.getSortedModules().size());
    }

    @Test
    void testGreetingService() {
        GreetingService service = container.getBeanRegistry().getBean(GreetingService.class);
        
        assertNotNull(service);
        assertTrue(service.isInitialized());
        assertEquals("Hello, World!", service.greet("World"));
    }

    @Test
    void testSingleton() {
        GreetingService s1 = container.getBeanRegistry().getBean(GreetingService.class);
        GreetingService s2 = container.getBeanRegistry().getBean(GreetingService.class);
        
        assertSame(s1, s2);
    }
}