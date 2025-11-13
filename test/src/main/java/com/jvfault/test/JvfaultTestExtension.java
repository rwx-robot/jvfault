package com.jvfault.test;

import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.module.ModuleContainer;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * Jvfault 测试扩展 - JUnit 5 集成
 *
 * <p>使用示例：
 * <pre>{@code
 * @ExtendWith(JvfaultTestExtension.class)
 * @TestModule(AppModule.class)
 * class MyServiceTest {
 *
 *     @Autowired
 *     private MyService myService;
 *
 *     @Test
 *     void testSomething(@Autowired MyService service) { ... }
 * }
 * }</pre>
 *
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
public class JvfaultTestExtension implements BeforeAllCallback, AfterAllCallback,
        TestInstancePostProcessor, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(JvfaultTestExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        // 查找测试类上的 @TestModule 注解
        Class<?> testClass = context.getRequiredTestClass();
        TestModule testModule = testClass.getAnnotation(TestModule.class);

        Class<?>[] modules = testModule != null ? testModule.value() : new Class<?>[]{testClass};
        String[] basePackages = testModule != null ? testModule.basePackages() : new String[0];

        ModuleContainer container = JvfaultApplication.createContainer(modules[0], basePackages);
        context.getRoot().getStore(NAMESPACE).put("container", container);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        ModuleContainer container = context.getRoot().getStore(NAMESPACE)
                .get("container", ModuleContainer.class);
        if (container != null) {
            container.destroy();
        }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        ModuleContainer container = context.getRoot().getStore(NAMESPACE)
                .get("container", ModuleContainer.class);
        if (container == null) {
            return;
        }
        // 字段注入
        Class<?> clazz = testInstance.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    field.set(testInstance, container.getBeanRegistry().getBean(field.getType()));
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to inject field: "
                            + clazz.getName() + "." + field.getName(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.isAnnotated(Autowired.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        ModuleContainer container = extensionContext.getRoot().getStore(NAMESPACE)
                .get("container", ModuleContainer.class);
        Parameter parameter = parameterContext.getParameter();
        return container.getBeanRegistry().getBean(parameter.getType());
    }
}
