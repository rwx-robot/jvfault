package com.jvfault.example.v1000;

import com.jvfault.logging.JsonLogFormatter;
import com.jvfault.logging.KeyValuePair;
import com.jvfault.logging.StructuredLogger;
import com.jvfault.logging.StructuredLoggerFactory;
import com.jvfault.nativeimage.NativeRuntimeHints;

public class Application {

    public static void main(String[] args) {
        System.out.println("== jvfault v0.10.0: Structured Logging + Native ==");
        StructuredLogger logger = new StructuredLoggerFactory(new JsonLogFormatter())
                .getLogger(Application.class);

        logger.info("order.created",
                KeyValuePair.of("orderId", "A-1024"),
                KeyValuePair.of("amount", 199.0));
        logger.warn("slow.query", KeyValuePair.of("ms", 812));

        String reflectConfig = new NativeRuntimeHints()
                .register(Application.class, StructuredLogger.class)
                .generateReflectConfig();
        System.out.println("  reflect-config 条目数: " + (reflectConfig.split("name").length - 1));
        System.out.println("  native 模式: " + NativeRuntimeHints.isNative());
        System.out.println("== 完成 ==");
    }
}
