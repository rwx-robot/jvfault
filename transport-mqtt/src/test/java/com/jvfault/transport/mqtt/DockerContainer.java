package com.jvfault.transport.mqtt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 测试支撑：通过 docker CLI 管理一次性 Mosquitto 容器
 * （1.6 默认监听 0.0.0.0 且允许匿名，无需挂载配置）。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
final class DockerContainer implements AutoCloseable {

    private static final String IMAGE = "eclipse-mosquitto:1.6";
    private static final String BOOTSTRAP = "tcp://localhost:1883";

    private String containerId;

    DockerContainer start() throws IOException, InterruptedException {
        ensureImage();
        Result run = execCapture("docker", "run", "-d", "--rm", "--publish=1883:1883", IMAGE);
        if (run.exitCode != 0) {
            // 可能是端口已被本机既有 broker 占用 —— 若能连通就直接复用，
            // 此时 containerId 保持 null，close() 不会误删外部容器。
            if (brokerReady()) {
                return this;
            }
            throw new IllegalStateException("docker run 失败(" + run.exitCode + "): " + run.output);
        }
        containerId = run.output;
        long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
        while (System.nanoTime() < deadline) {
            if (brokerReady()) {
                return this;
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
        throw new IllegalStateException("Mosquitto 启动超时: " + IMAGE);
    }

    String getBootstrap() {
        return BOOTSTRAP;
    }

    private boolean brokerReady() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 1883), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureImage() throws IOException, InterruptedException {
        // 注意：exec 合并了 stderr —— 镜像不存在时 docker 会把
        // "Error response from daemon: No such image" 写到 stdout，
        // 所以**不能**用「输出是否为空」判断存在性，必须看**退出码**。
        if (execExitCode("docker", "image", "inspect", "--format", "{{.Id}}", IMAGE) != 0) {
            throw new IllegalStateException("本地无镜像 " + IMAGE + "，请先 docker pull");
        }
    }

    static boolean hasImage() {
        try {
            new DockerContainer().ensureImage();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean dockerAvailable() {
        try {
            exec("docker", "version");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {
        if (containerId != null) {
            try {
                exec("docker", "rm", "-f", containerId);
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    private static String exec(String... cmd) throws IOException, InterruptedException {
        return execCapture(cmd).output;
    }

    /** 执行并返回退出码（stdout 已 drain，避免子进程因管道写满而阻塞）。 */
    private static int execExitCode(String... cmd) throws IOException, InterruptedException {
        return execCapture(cmd).exitCode;
    }

    private static Result execCapture(String... cmd) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = process.getInputStream().read(chunk)) > 0) {
            buffer.write(chunk, 0, n);
        }
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("docker 命令超时: " + String.join(" ", cmd));
        }
        return new Result(process.exitValue(),
                new String(buffer.toByteArray(), StandardCharsets.UTF_8).trim());
    }

    private static final class Result {
        final int exitCode;
        final String output;

        Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
