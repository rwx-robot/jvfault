package com.jvfault.transport.kafka;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** docker CLI 管理容器（避免 Testcontainers 拉镜像超时） */
final class DockerContainer implements AutoCloseable {

    private String containerId;
    private final String image;
    private final String internalPort;

    DockerContainer(String image, String internalPort) {
        this.image = image;
        this.internalPort = internalPort;
    }

    DockerContainer start(String... extraEnv) throws IOException, InterruptedException {
        DockerContainer.ensureImage(image);
        java.util.List<String> cmd = new java.util.ArrayList<>(java.util.Arrays.asList(
                "docker", "run", "-d", "--rm", "--publish=9092:9092"));
        for (String e : extraEnv) {
            cmd.add("-e");
            cmd.add(e);
        }
        cmd.add(image);
        containerId = DockerContainer.exec(cmd.toArray(new String[0])).trim();
        long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
        while (System.nanoTime() < deadline) {
            String port = hostPort();
            if (!port.isEmpty()) {
                return this;
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
        throw new IllegalStateException("Kafka 启动超时: " + image);
    }

    String hostPort() throws IOException, InterruptedException {
        return DockerContainer.exec("docker", "port", containerId, internalPort).trim();
    }

    static void ensureImage(String image) throws IOException, InterruptedException {
        String status = DockerContainer.exec("docker", "image", "inspect", "--format", "{{.Id}}", image).trim();
        if (status.isEmpty()) {
            throw new IllegalStateException("本地无镜像 " + image + "，请先 docker pull");
        }
    }

    @Override
    public void close() {
        if (containerId != null) {
            try {
                DockerContainer.exec("docker", "rm", "-f", containerId);
            } catch (Exception ignored) {
            }
        }
    }

    static boolean dockerAvailable() {
        try {
            DockerContainer.exec("docker", "version");
            return true;
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
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("docker 命令超时: " + String.join(" ", cmd));
        }
        return new String(buffer.toByteArray(), "UTF-8").trim();
    }
}
