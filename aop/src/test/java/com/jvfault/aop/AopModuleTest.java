package com.jvfault.aop;

import com.jvfault.aop.aspect.Advisor;
import com.jvfault.aop.aspect.AspectRegistry;
import com.jvfault.aop.pointcut.PointcutExpression;
import com.jvfault.aop.proxy.ProxyFactory;
import com.jvfault.bootstrap.AopBootstrap;
import com.jvfault.bootstrap.Calculator;
import com.jvfault.bootstrap.CalculatorInterface;
import com.jvfault.bootstrap.TestAspects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-aop 核心测试
 *
 * @since v0.2.0 (2016)
 */
@DisplayName("AOP 模块测试")
class AopModuleTest {

    // ============ PointcutExpression ============

    @Test
    @DisplayName("pointcut: 通配段与方法名前后缀")
    void testPointcutWildcards() throws Exception {
        PointcutExpression expr = PointcutExpression.parse(
                "execution(* com.example.service.*Service.save*(..))");

        Class<?> svc = com.example.service.OrderService.class;
        Method save = svc.getMethod("saveOrder", String.class);
        Method list = svc.getMethod("listAll");

        assertTrue(expr.matches(svc, save));
        assertFalse(expr.matches(svc, list));
    }

    @Test
    @DisplayName("pointcut: .. 跨段与无参匹配")
    void testPointcutMultiSegment() throws Exception {
        PointcutExpression cross = PointcutExpression.parse(
                "execution(* com.example..*Controller.handle(..))");
        Class<?> ctrl = com.example.web.user.UserController.class;
        Method handle = ctrl.getMethod("handle", String.class);
        assertTrue(cross.matches(ctrl, handle));

        PointcutExpression noArgs = PointcutExpression.parse(
                "execution(* com.example.web.user.UserController.ping())");
        assertFalse(noArgs.matches(ctrl, handle));
        assertTrue(noArgs.matches(ctrl, ctrl.getMethod("ping")));
    }

    @Test
    @DisplayName("pointcut: 非法表达式抛异常")
    void testPointcutIllegal() {
        assertThrows(IllegalArgumentException.class, () -> PointcutExpression.parse("bean(*Service)"));
        assertThrows(IllegalArgumentException.class, () -> PointcutExpression.parse("execution(nosep)"));
        assertThrows(IllegalArgumentException.class,
                () -> PointcutExpression.parse("execution(* ..(..))"));
    }

    // ============ 代理与通知语义 ============

    @Test
    @DisplayName("接口代理: @Around 环绕前后")
    void testJdkProxyAround() {
        TestAspects.LoggingAspect aspect = new TestAspects.LoggingAspect();
        ProxyFactory factory = new ProxyFactory(Arrays.asList(new Advisor(aspect)));

        CalculatorInterface calc = factory.createProxy(new Calculator());
        assertEquals(5, calc.add(2, 3));
        assertEquals("[before:add][after:add]", aspect.log.toString());
    }

    @Test
    @DisplayName("类代理 (ByteBuddy): 无接口目标")
    void testClassProxy() {
        TestAspects.LoggingAspect aspect = new TestAspects.LoggingAspect();
        ProxyFactory factory = new ProxyFactory(Arrays.asList(new Advisor(aspect)));

        Calculator calc = factory.createProxy(new Calculator());
        assertNotNull(calc);
        assertNotEquals(Calculator.class, calc.getClass());
        assertEquals(6, calc.mul(2, 3));
        assertEquals("[before:mul][after:mul]", aspect.log.toString());
    }

    @Test
    @DisplayName("@Around 改写返回值")
    void testAroundRewritesResult() {
        ProxyFactory factory = new ProxyFactory(
                Arrays.asList(new Advisor(new TestAspects.TransformAspect())));
        CalculatorInterface calc = factory.createProxy(new Calculator());
        assertEquals("HELLO!", calc.shout("hello"));
    }

    @Test
    @DisplayName("@Before/@After/@AfterThrowing 语义")
    void testBeforeAfterThrowing() {
        TestAspects.CountingAspect aspect = new TestAspects.CountingAspect();
        ProxyFactory factory = new ProxyFactory(Arrays.asList(new Advisor(aspect)));
        CalculatorInterface calc = factory.createProxy(new Calculator());

        calc.add(1, 2);
        assertEquals(1, aspect.calls.get());

        calc.div(6, 3);          // 正常: @After +10
        assertEquals(11, aspect.calls.get());

        assertThrows(ArithmeticException.class, () -> calc.div(1, 0)); // +10 +100
        assertEquals(121, aspect.calls.get());
    }

    @Test
    @DisplayName("未匹配方法透传")
    void testUnmatchedPassthrough() {
        // TransformAspect 只切 shout，add 未被任何通知命中 -> 完全透传
        TestAspects.TransformAspect aspect = new TestAspects.TransformAspect();
        ProxyFactory factory = new ProxyFactory(Arrays.asList(new Advisor(aspect)));
        CalculatorInterface calc = factory.createProxy(new Calculator());
        assertEquals(3, calc.abs(-3));
        assertEquals("HELLO!", calc.shout("hello"));
    }

    @Test
    @DisplayName("Advisor 按 @Order 排序")
    void testAdvisorOrder() {
        AspectRegistry registry = new AspectRegistry();
        registry.registerAspect(new TestAspects.CountingAspect());   // @Order(2)
        registry.registerAspect(new TestAspects.LoggingAspect());    // @Order(1)
        registry.registerAspect(new Object());                       // 非 @Aspect 忽略

        List<Advisor> advisors = registry.getAdvisors();
        assertEquals(2, advisors.size());
        assertEquals(TestAspects.LoggingAspect.class, advisors.get(0).getAspectInstance().getClass());
    }

    @Test
    @DisplayName("final 类回退原对象")
    void testFinalClassFallback() {
        ProxyFactory factory = new ProxyFactory(
                Arrays.asList(new Advisor(new TestAspects.LoggingAspect())));
        String target = "hello";
        assertSame(target, factory.createProxy(target));
    }

    // ============ 与 core 容器集成 ============

    @Test
    @DisplayName("容器集成: 切面自动代理目标 Bean")
    void testContainerIntegration() {
        com.jvfault.core.module.ModuleContainer container =
                com.jvfault.core.bootstrap.JvfaultApplication.createContainer(AopBootstrap.class);
        try {
            CalculatorInterface calc = container.getBeanRegistry().getBean(CalculatorInterface.class);
            assertNotNull(calc);

            TestAspects.LoggingAspect logging =
                    container.getBeanRegistry().getBean(TestAspects.LoggingAspect.class);
            TestAspects.CountingAspect counting =
                    container.getBeanRegistry().getBean(TestAspects.CountingAspect.class);

            calc.div(10, 2);
            assertTrue(logging.log.toString().contains("[before:div]"));

            int beforeAdd = counting.calls.get();
            calc.add(1, 1);
            assertEquals(beforeAdd + 1, counting.calls.get());

            // 同一 Bean 再次获取仍是同一代理实例
            CalculatorInterface again = container.getBeanRegistry().getBean(CalculatorInterface.class);
            assertSame(calc, again);
        } finally {
            container.destroy();
        }
    }
}
