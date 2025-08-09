package com.jvfault.example.v010;

import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.module.ModuleContainer;

/**
 * v0.1.0 示例应用入口
 * 演示核心 IoC 容器功能
 * 
 * 运行方式:
 *   ./gradlew :examples:v0.1.0:run
 * 
 * 或构建后运行:
 *   java -jar examples/v0.1.0/build/libs/v0.1.0-0.1.0-SNAPSHOT-all.jar
 */
public class Application {

    public static void main(String[] args) {
        JvfaultApplication.printBanner();
        
        System.out.println("🚀 Starting Jvfault v0.1.0 Example Application...\n");

        // 启动容器
        ModuleContainer container = JvfaultApplication.run(AppModule.class, args);

        try {
            // 获取 Bean 并测试
            GreetingService greetingService = container.getBeanRegistry().getBean(GreetingService.class);
            
            System.out.println("\n📋 Testing Dependency Injection:");
            System.out.println("   " + greetingService.greet("World"));
            System.out.println("   " + greetingService.greet("Jvfault"));
            System.out.println("   Initialized: " + greetingService.isInitialized());
            
            // 测试单例
            GreetingService another = container.getBeanRegistry().getBean(GreetingService.class);
            System.out.println("\n🔄 Singleton Test: " + (greetingService == another ? "PASS" : "FAIL"));
            
            // 打印容器信息
            System.out.println("\n📊 Container Info:");
            System.out.println("   Modules: " + container.getSortedModules().size());
            System.out.println("   Beans: " + container.getBeanRegistry().getBeanNames().size());
            for (String beanName : container.getBeanRegistry().getBeanNames()) {
                System.out.println("     - " + beanName + " [" + container.getBeanRegistry().getType(beanName).getSimpleName() + "]");
            }

            System.out.println("\n✅ Application running successfully!");
            System.out.println("   Press Ctrl+C to shutdown...\n");

            // 保持运行直到关闭
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Application error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            container.destroy();
        }
    }
}