package com.jvfault.example.v070;

import com.jvfault.cache.annotation.Cacheable;
import com.jvfault.cache.cache.DefaultCacheManager;
import com.jvfault.cache.CacheableBeanPostProcessor;
import com.jvfault.core.annotation.Component;
import com.jvfault.openapi.OpenApiGenerator;
import com.jvfault.scheduling.annotation.Scheduled;
import com.jvfault.scheduling.trigger.CronTrigger;
import com.jvfault.web.WebApplication;
import com.jvfault.web.annotation.Controller;
import com.jvfault.web.annotation.Get;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Application {

    @Component
    @Controller("/products")
    static class ProductController {
        @Get
        public String list() {
            return "[]";
        }

        @Get(":id")
        public String detail(@com.jvfault.web.annotation.Param("id") long id) {
            return "product-" + id;
        }
    }

    interface PriceService {
        String price(String sku);
    }

    @Component
    static class PriceServiceImpl implements PriceService {
        int calls;

        @Override
        @Cacheable(cacheNames = "prices", key = "#p0")
        public String price(String sku) {
            return "USD 9.99 #" + (++calls);
        }
    }

    static class Jobs {
        @Scheduled(cron = "0 0 9 * * MON-FRI", name = "morning-report")
        public void morningReport() {
        }
    }

    public static void main(String[] args) {
        System.out.println("== jvfault v0.7.0: OpenAPI + Scheduling + Cache ==");

        WebApplication app = new WebApplication(new com.jvfault.core.container.DefaultBeanRegistry());
        app.registerController(new ProductController());
        String openapi = new OpenApiGenerator(app).title("Shop API").version("0.7.0").toJson();
        System.out.println("  OpenAPI 包含 /products 与 /products/{id}: "
                + openapi.contains("/products/{id}"));

        // cron 解析演示
        CronTrigger cron = new CronTrigger("0 0 9 * * MON-FRI");
        ZonedDateTime next = cron.next(ZonedDateTime.of(2021, 5, 14, 18, 0, 0, 0, ZoneId.of("Asia/Shanghai")));
        System.out.println("  Cron 下次执行: " + next);

        // 缓存
        DefaultCacheManager cacheManager = new DefaultCacheManager();
        CacheableBeanPostProcessor bpp = new CacheableBeanPostProcessor(cacheManager);
        PriceServiceImpl impl = new PriceServiceImpl();
        PriceService proxied = (PriceService) bpp.postProcessAfterInitialization(impl, "priceService");
        String first = proxied.price("SKU-1");
        String second = proxied.price("SKU-1");
        System.out.println("  缓存命中: " + first + " -> " + second + " (方法执行次数: " + impl.calls + ")");

        System.out.println("== 完成 ==");
    }
}
