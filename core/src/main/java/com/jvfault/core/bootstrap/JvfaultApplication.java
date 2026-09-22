package com.jvfault.core.bootstrap;

import com.jvfault.core.container.BeanRegistry;
import com.jvfault.core.container.DefaultBeanRegistry;
import com.jvfault.core.module.ModuleContainer;
import com.jvfault.core.scanner.ModuleScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * jvfault 引导类 - 应用程序启动入口
 * 
 * <p>使用示例：
 * <pre>{@code
 * public class Application {
 *     public static void main(String[] args) {
 *         JvfaultApplication.run(AppModule.class, args);
 *     }
 * }
 * }</pre>
 * 
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
public final class JvfaultApplication {

    private static final Logger log = LoggerFactory.getLogger(JvfaultApplication.class);

    private JvfaultApplication() {}

    /**
     * 运行应用 (阻塞直到关闭)
     * 
     * @param rootModuleClass 根模块类
     * @param args 命令行参数
     * @return ModuleContainer 容器实例 (用于测试或扩展)
     */
    public static ModuleContainer run(Class<?> rootModuleClass, String... args) {
        return run(rootModuleClass, null, args);
    }

    /**
     * 运行应用 (指定基础包)
     * 
     * @param rootModuleClass 根模块类
     * @param basePackages 扫描基础包 (默认: 根模块所在包)
     * @param args 命令行参数
     * @return ModuleContainer 容器实例
     */
    public static ModuleContainer run(Class<?> rootModuleClass, String[] basePackages, String... args) {
        long start = System.nanoTime();
        log.info("Starting Jvfault Application v{}...", getVersion());

        // 解析基础包
        String[] packages = basePackages != null && basePackages.length > 0
                ? basePackages
                : new String[]{defaultPackage(rootModuleClass)};

        // 创建容器
        BeanRegistry beanRegistry = new DefaultBeanRegistry();
        ModuleContainer container = new ModuleContainer(beanRegistry);

        try {
            // 刷新容器 (核心初始化流程)
            container.refresh(rootModuleClass, packages);

            // 注册关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down Jvfault Application...");
                container.destroy();
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                log.info("Application stopped in {}ms", elapsed);
            }));

            log.info("Jvfault Application started in {}ms", (System.nanoTime() - start) / 1_000_000);
            return container;

        } catch (Exception e) {
            log.error("Application startup failed", e);
            container.destroy();
            throw new IllegalStateException("Application startup failed", e);
        }
    }

    /**
     * 创建容器但不刷新 (用于测试)
     */
    public static ModuleContainer createContainer(Class<?> rootModuleClass, String... basePackages) {
        String[] packages = basePackages.length > 0
                ? basePackages
                : new String[]{defaultPackage(rootModuleClass)};
        
        BeanRegistry beanRegistry = new DefaultBeanRegistry();
        ModuleContainer container = new ModuleContainer(beanRegistry);
        container.refresh(rootModuleClass, packages);
        return container;
    }

    /**
     * 获取框架版本
     */
    public static String getVersion() {
        Package pkg = JvfaultApplication.class.getPackage();
        String version = pkg != null ? pkg.getImplementationVersion() : null;
        return version != null ? version : "0.1.0";
    }

    /**
     * 打印启动横幅
     */
    public static void printBanner() {
        System.out.println(
              "  __  __ _           _     _ \n"
            + " |  \\/  (_)_ __  ___| |__ (_)\n"
            + " | |\\/| | | '_ \\/ __| '_ \\| |\n"
            + " | |  | | | | | \\__ \\ | | | |\n"
            + " |_|  |_|_|_| |_|___/_| |_|_|\n"
            + "\n"
            + "jvfault - Modern Modular Java Framework");
    }

    private static String defaultPackage(Class<?> clazz) {
        Package pkg = clazz.getPackage();
        return pkg != null ? pkg.getName() : "";
    }
}