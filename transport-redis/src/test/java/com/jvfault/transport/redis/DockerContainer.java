package com.jvfault.transport.redis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 测试支撑：通过 docker CLI 管理一次性容器
 * （不依赖 Testcontainers 镜像拉取策略，本地已有镜像即可运行）。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
final class DockerContainer implements AutoCloseable {

    private String containerId;
    private final String image;
    private final String internalPort;

    DockerContainer(String image, String internalPort) {
        this.image = image;
        this.internalPort = internalPort;
    }

    DockerContainer start() throws IOException, InterruptedException {
        ensureImage();
        containerId = exec("docker", "run", "-d", "--rm", "-P", image).trim();
        // 等待端口就绪
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            if (!hostPort().isEmpty()) {
                return this;
            }
            TimeUnit.MILLISECONDS.sleep(200);
        }
        throw new IllegalStateException("容器端口未就绪: " + image);
    }

    /** 宿主机映射端口 */
    String hostPort() throws IOException, InterruptedException {
        return exec("docker", "port", containerId, internalPort).trim();
    }

    private void ensureImage() throws IOException, InterruptedException {
        ensureImage(image);
    }

    static void ensureImage(String image) throws IOException, InterruptedException {
        if (!hasImage(image)) {
            throw new IllegalStateException("本地无镜像 " + image + "，请先 docker pull");
        }
    }

    /**
     * 镜像是否已存在于本地。
     * <p>注意：{@code exec} 在退出码非 0 时返回空串，因此用「输出非空」判断
     * 即可正确区分「镜像存在」与「No such image」。
     */
    static boolean hasImage(String image) throws IOException, InterruptedException {
        return !exec("docker", "image", "inspect", "--format", "{{.Id}}", image).isEmpty();
    }

    @Override
    public void close() {
        if (containerId != null) {
            try {
                exec("docker", "rm", "-f", containerId);
            } catch (Exception ignored) {
            }
        }
    }

    static boolean dockerAvailable() {
        try {
            // exec 在非 0 退出码时返回空串 —— 需显式判空，
            // 否则 daemon 未启动也会被判为"可用"。
            return !exec("docker", "version").isEmpty();
        } catch (Exception e) {
            return false;
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
        byte[] output = buffer.toByteArray();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("docker 命令超时: " + String.join(" ", cmd));
        }
        String text = new String(output, StandardCharsets.UTF_8).trim();
        if (process.exitValue() != 0) {
            return "";
        }
        return text;
    }
}
