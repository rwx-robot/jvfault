package com.jvfault.bootstrap;

import com.jvfault.aop.annotation.After;
import com.jvfault.aop.annotation.AfterThrowing;
import com.jvfault.aop.annotation.Around;
import com.jvfault.aop.annotation.Aspect;
import com.jvfault.aop.annotation.Before;
import com.jvfault.aop.annotation.Order;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * AOP 测试切面集合（public，供跨包引用）。
 */
public final class TestAspects {

    private TestAspects() {
    }

    @Aspect
    @Order(1)
    public static class LoggingAspect {
        public final StringBuilder log = new StringBuilder();

        @Around("execution(* com.jvfault.bootstrap..*(..))")
        public Object around(com.jvfault.aop.interception.MethodInvocation inv) throws Throwable {
            log.append("[before:").append(inv.getMethod().getName()).append("]");
            try {
                return inv.proceed();
            } finally {
                log.append("[after:").append(inv.getMethod().getName()).append("]");
            }
        }
    }

    @Aspect
    @Order(2)
    public static class CountingAspect {
        public final AtomicInteger calls = new AtomicInteger();

        @Before("execution(* com.jvfault.bootstrap.Calculator.add(..))")
        public void before() {
            calls.incrementAndGet();
        }

        @After("execution(* com.jvfault.bootstrap.Calculator.div(..))")
        public void after() {
            calls.addAndGet(10);
        }

        @AfterThrowing("execution(* com.jvfault.bootstrap.Calculator.div(..))")
        public void onError(ArithmeticException ex) {
            calls.addAndGet(100);
        }
    }

    @Aspect
    public static class TransformAspect {
        @Around("execution(String com.jvfault.bootstrap.Calculator.shout(..))")
        public Object upper(com.jvfault.aop.interception.MethodInvocation inv) throws Throwable {
            Object result = inv.proceed();
            return result == null ? null : result.toString().toUpperCase() + "!";
        }
    }
}
