package com.jvfault.virtualthreads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 虚拟线程执行器工厂。
 * 对应 roadmap v0.8.0: Virtual Thread Executor
 *
 * <p>注意：虚拟线程不做池化（创建成本极低），"固定大小"语义
 * 通过信号量限制同时在跑的任务数（防下游资源被打爆）。
 *
 * @since v0.8.0 (2022)
 * @author jvfault team
 */
public final class VirtualThreadExecutors {

    private VirtualThreadExecutors() {
    }

    /**
     * 每任务一个虚拟线程（无上限）。
     */
    public static ExecutorService newPerTaskExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    }

    /**
     * 有并发上限的虚拟线程执行器（信号量限流）。
     *
     * @param maxConcurrent 最大并发任务数
     */
    public static ExecutorService newBoundedExecutor(int maxConcurrent) {
        if (maxConcurrent <= 0) {
            throw new IllegalArgumentException("maxConcurrent 必须为正");
        }
        Semaphore permits = new Semaphore(maxConcurrent);
        return Executors.newThreadPerTaskExecutor(task -> {
            Runnable wrapped = () -> {
                permits.acquireUninterruptibly();
                try {
                    task.run();
                } finally {
                    permits.release();
                }
            };
            return Thread.ofVirtual().unstarted(wrapped);
        });
    }

    /** 当前线程是否为虚拟线程 */
    public static boolean isVirtual() {
        return Thread.currentThread().isVirtual();
    }
}
