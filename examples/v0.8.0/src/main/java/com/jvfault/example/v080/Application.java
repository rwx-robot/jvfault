package com.jvfault.example.v080;

import com.jvfault.virtualthreads.StructuredParallel;
import com.jvfault.virtualthreads.VirtualThreadExecutors;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Application {

    public static void main(String[] args) throws Exception {
        System.out.println("== jvfault v0.8.0: Virtual Threads ==");
        System.out.println("  main 是虚拟线程: " + VirtualThreadExecutors.isVirtual());

        // 10k 虚拟线程并发任务（阻塞式 sleep 模拟 IO）
        int tasks = 10_000;
        AtomicInteger done = new AtomicInteger();
        ExecutorService executor = VirtualThreadExecutors.newPerTaskExecutor();
        long start = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(tasks);
        for (int i = 0; i < tasks; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                done.incrementAndGet();
                latch.countDown();
            });
        }
        latch.await(30, TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("  " + tasks + " 个并发阻塞任务完成于 " + elapsedMs + "ms（载体线程仅 CPU 核数个）");
        executor.shutdown();

        // 结构化并发：并行聚合两个分支
        long parallelStart = System.nanoTime();
        List<Integer> results = StructuredParallel.invokeAll(List.of(
                (Callable<Integer>) () -> {
                    Thread.sleep(100);
                    return 19;
                },
                (Callable<Integer>) () -> {
                    Thread.sleep(100);
                    return 23;
                }));
        System.out.println("  结构化并行 2×100ms 分支耗时 "
                + (System.nanoTime() - parallelStart) / 1_000_000 + "ms（并行则 <200ms）, sum="
                + (results.get(0) + results.get(1)));
        System.out.println("== 完成 ==");
    }
}
