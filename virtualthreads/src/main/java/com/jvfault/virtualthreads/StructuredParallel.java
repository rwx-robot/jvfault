package com.jvfault.virtualthreads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;

/**
 * 结构化并发辅助 —— 在虚拟线程上并行执行任务分支：
 * 全部成功返回结果列表；任一失败取消其余分支并抛出首个异常。
 * 对应 roadmap v0.8.0: Structured Concurrency（JEP 453 的非预览实现）
 *
 * @since v0.8.0 (2022)
 * @author jvfault team
 */
public final class StructuredParallel {

    private StructuredParallel() {
    }

    /**
     * 并行执行全部分支。
     *
     * @param <T>      统一结果类型
     * @param branches 分支任务（数量 ≥ 1）
     * @return 与分支顺序一致的结果列表
     * @throws IllegalStateException 任一分支失败（cause 为原始异常）
     */
    public static <T> List<T> invokeAll(List<Callable<T>> branches) {
        if (branches == null || branches.isEmpty()) {
            throw new IllegalArgumentException("至少一个分支");
        }
        try (ExecutorService executor = VirtualThreadExecutors.newPerTaskExecutor()) {
            List<Future<T>> futures = new ArrayList<>(branches.size());
            for (Callable<T> branch : branches) {
                futures.add(executor.submit(branch));
            }
            List<T> results = new ArrayList<>(branches.size());
            Throwable firstFailure = null;
            for (Future<T> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    futures.forEach(f -> f.cancel(true));
                    throw new IllegalStateException("结构化并发被中断", e);
                } catch (ExecutionException e) {
                    if (firstFailure == null) {
                        firstFailure = e.getCause();
                    }
                }
            }
            if (firstFailure != null) {
                futures.forEach(f -> f.cancel(true));
                throw new IllegalStateException("分支失败: " + firstFailure.getMessage(), firstFailure);
            }
            return results;
        }
    }

    /** 双分支便捷重载 */
    public static <A, B> java.util.Map.Entry<A, B> invokeAll(Callable<A> first, Callable<B> second) {
        List<Object> results = invokeAll(java.util.List.of(
                (Callable<Object>) first, (Callable<Object>) second));
        return java.util.Map.entry((A) results.get(0), (B) results.get(1));
    }
}
