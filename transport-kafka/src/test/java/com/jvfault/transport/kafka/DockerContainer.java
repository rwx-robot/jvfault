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
        Result run = DockerContainer.execCapture(cmd.toArray(new String[0]));
        if (run.exitCode != 0) {
            // 可能是端口已被本机既有 broker 占用 —— 若能连通就直接复用，
            // 此时 containerId 保持 null，close() 不会误删外部容器。
            if (DockerContainer.stdPortReady()) {
                return this;
            }
            throw new IllegalStateException("docker run 失败(" + run.exitCode + "): " + run.output);
        }
        containerId = run.output;
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

    /** 本机 9092 是否已有 Kafka 在监听（用于 docker run 端口冲突时复用）。 */
    private static boolean stdPortReady() {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("localhost", 9092), 1_000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void ensureImage(String image) throws IOException, InterruptedException {
        // 注意：exec 合并了 stderr —— 镜像不存在时 docker 会把
        // "Error response from daemon: No such image" 写到 stdout，
        // 所以**不能**用「输出是否为空」判断存在性，必须看**退出码**。
        if (DockerContainer.execExitCode("docker", "image", "inspect", "--format", "{{.Id}}", image) != 0) {
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
        return DockerContainer.execCapture(cmd).output;
    }

    /** 执行并返回退出码（stdout 已 drain，避免子进程因管道写满而阻塞）。 */
    private static int execExitCode(String... cmd) throws IOException, InterruptedException {
        return DockerContainer.execCapture(cmd).exitCode;
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
                new String(buffer.toByteArray(), "UTF-8").trim());
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
