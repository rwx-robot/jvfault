package com.jvfault.example.v020;

import com.jvfault.aop.AopModule;
import com.jvfault.aop.annotation.Around;
import com.jvfault.aop.annotation.Aspect;
import com.jvfault.aop.interception.MethodInvocation;
import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Module;
import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.module.ModuleContainer;

public class Application {

    @Aspect
    @Component
    static class TimingAspect {
        @Around("execution(* com.jvfault.example.v020..*Service.compute(..))")
        public Object time(MethodInvocation invocation) throws Throwable {
            long start = System.nanoTime();
            try {
                return invocation.proceed();
            } finally {
                System.out.println("  [aspect] " + invocation.getMethod().getName()
                        + " took " + (System.nanoTime() - start) / 1_000 + "us");
            }
        }
    }

    interface Calculator {
        int compute(int a, int b);
    }

    @Component
    static class AddService implements Calculator {
        public int compute(int a, int b) {
            return a + b;
        }
    }

    @Module(imports = AopModule.class, providers = {TimingAspect.class, AddService.class})
    static class AppModule {
    }

    public static void main(String[] args) {
        System.out.println("== jvfault v0.2.0: AOP ==");
        ModuleContainer container = JvfaultApplication.createContainer(AppModule.class);
        try {
            Calculator calc = container.getBeanRegistry().getBean(Calculator.class);
            System.out.println("  compute(19, 23) = " + calc.compute(19, 23));
            System.out.println("  同一实例再取: " + (calc == container.getBeanRegistry().getBean(Calculator.class) ? "单例 PASS" : "FAIL"));
        } finally {
            container.destroy();
        }
        System.out.println("== 完成 ==");
    }
}
