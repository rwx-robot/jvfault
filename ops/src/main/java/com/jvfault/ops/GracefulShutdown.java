package com.jvfault.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 优雅关闭协调器 —— 注册关闭钩子，SIGTERM 时按注册顺序执行。
 *
 * @since v1.0.0 (2026)
 * @author jvfault team
 */
public class GracefulShutdown {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdown.class);

    private final List<Runnable> hooks = new CopyOnWriteArrayList<>();
    private final CountDownLatch terminated = new CountDownLatch(1);
    private volatile boolean shuttingDown;

    /**
     * 安装 JVM shutdown hook（调用一次）。
     */
    public GracefulShutdown install() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shuttingDown = true;
            log.info("收到关闭信号，执行 {} 个钩子", hooks.size());
            for (Runnable hook : hooks) {
                try {
                    hook.run();
                } catch (Exception e) {
                    log.warn("关闭钩子失败", e);
                }
            }
            terminated.countDown();
        }, "jvfault-graceful-shutdown"));
        return this;
    }

    public GracefulShutdown addHook(Runnable hook) {
        hooks.add(hook);
        return this;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * 等待关闭完成。
     */
    public boolean awaitTermination(long timeoutMillis) throws InterruptedException {
        return terminated.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public int hookCount() {
        return hooks.size();
    }

    /** 供测试/工具直接触发钩子（不走 JVM hook） */
    public List<Runnable> getHooks() {
        return new ArrayList<>(hooks);
    }
}
