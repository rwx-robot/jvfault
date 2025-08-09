package com.jvfault.example.v030;

import com.jvfault.config.ConfigModule;
import com.jvfault.config.annotation.ConfigurationProperties;
import com.jvfault.config.annotation.Value;
import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Module;
import com.jvfault.core.bootstrap.JvfaultApplication;
import com.jvfault.core.module.ModuleContainer;

public class Application {

    @Component
    @ConfigurationProperties("app")
    static class AppConfig {
        String name;
        int timeout;
    }

    @Component
    static class DbService {
        @Value("db.url")
        String url;
    }

    @Module(imports = ConfigModule.class, providers = {AppConfig.class, DbService.class})
    static class AppModule {
    }

    public static void main(String[] args) {
        System.out.println("== jvfault v0.3.0: Config ==");
        System.out.println("  激活 profile: " + System.getProperty("jvfault.profiles", "(none)"));
        ModuleContainer container = JvfaultApplication.createContainer(AppModule.class);
        try {
            AppConfig config = container.getBeanRegistry().getBean(AppConfig.class);
            DbService db = container.getBeanRegistry().getBean(DbService.class);
            System.out.println("  app.name    = " + config.name);
            System.out.println("  app.timeout = " + config.timeout + (config.timeout >= 60 ? " (prod 覆盖)" : ""));
            System.out.println("  db.url      = " + db.url);
        } finally {
            container.destroy();
        }
        System.out.println("== 完成 ==");
    }
}
