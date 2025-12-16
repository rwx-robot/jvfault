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
        containerId = exec("docker", "run", "-d", "--rm", "--publish=1883:1883", IMAGE).trim();
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
        String status = exec("docker", "image", "inspect", "--format", "{{.Id}}", IMAGE).trim();
        if (status.isEmpty()) {
            throw new IllegalStateException("本地无镜像 " + IMAGE + "，请先 docker pull");
        }
    }

    static boolean hasImage() {
        try {
            exec("docker", "image", "inspect", "--format", "{{.Id}}", IMAGE);
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
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8).trim();
    }
}
