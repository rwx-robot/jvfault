package com.jvfault.transport.grpc;

import io.grpc.MethodDescriptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * gRPC 传输的通用方法描述符与地址解析工具。
 *
 * <p>不依赖 protobuf 代码生成：直接以 {@code byte[]} 作为请求/响应类型，
 * 复用 jvfault 的 JSON wire，一个 unary 方法承载全部 pattern 的调用。
 *
 * @since v1.0.2 (2026)
 * @author jvfault team
 */
public final class GrpcMethod {

    public static final String SERVICE_NAME = "jvfault.transport.JvfaultTransport";
    public static final String METHOD_NAME = "Invoke";

    /** 全局唯一的 unary 方法描述符（服务端/客户端共用）。 */
    public static final MethodDescriptor<byte[], byte[]> METHOD = MethodDescriptor.<byte[], byte[]>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, METHOD_NAME))
            .setRequestMarshaller(ByteArrayMarshaller.INSTANCE)
            .setResponseMarshaller(ByteArrayMarshaller.INSTANCE)
            .build();

    private GrpcMethod() {
    }

    /** 解析 bootstrap 的 host 部分，缺省 {@code localhost}。 */
    public static String host(String bootstrap) {
        if (bootstrap == null) {
            return "localhost";
        }
        String trimmed = bootstrap.trim();
        int idx = trimmed.lastIndexOf(':');
        if (idx <= 0) {
            return trimmed.isEmpty() ? "localhost" : trimmed;
        }
        return trimmed.substring(0, idx);
    }

    /** 解析 bootstrap 的 port 部分，缺省 {@code 0}（表示随机端口）。 */
    public static int port(String bootstrap) {
        if (bootstrap == null) {
            return 0;
        }
        String trimmed = bootstrap.trim();
        int idx = trimmed.lastIndexOf(':');
        if (idx < 0 || idx == trimmed.length() - 1) {
            return 0;
        }
        try {
            return Integer.parseInt(trimmed.substring(idx + 1).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** byte[] <-> InputStream 直通编组器（JSON wire 原样传输）。 */
    static final class ByteArrayMarshaller implements MethodDescriptor.Marshaller<byte[]> {

        static final ByteArrayMarshaller INSTANCE = new ByteArrayMarshaller();

        @Override
        public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(value);
        }

        @Override
        public byte[] parse(InputStream stream) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int n;
                while ((n = stream.read(buffer)) > 0) {
                    out.write(buffer, 0, n);
                }
                return out.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("gRPC 载荷读取失败", e);
            }
        }
    }
}
