package com.jvfault.tracing;

import java.security.SecureRandom;

/**
 * 追踪 ID 生成器 —— 基于 {@link SecureRandom} 生成 W3C 规范的小写十六进制 ID
 * 对应 OpenTelemetry: io.opentelemetry.api 内部随机 ID 生成
 *
 * <p>保证生成的 ID 不为全零（W3C Trace Context 要求）。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public final class IdGenerator {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    /**
     * 生成 32 位小写十六进制 traceId（16 字节，非全零）
     */
    public static String generateTraceId() {
        return generate(16);
    }

    /**
     * 生成 16 位小写十六进制 spanId（8 字节，非全零）
     */
    public static String generateSpanId() {
        return generate(8);
    }

    private static String generate(int byteCount) {
        byte[] bytes = new byte[byteCount];
        do {
            RANDOM.nextBytes(bytes);
        } while (allZero(bytes));

        char[] hex = new char[byteCount * 2];
        for (int i = 0; i < byteCount; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_CHARS[v >>> 4];
            hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hex);
    }

    private static boolean allZero(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}
