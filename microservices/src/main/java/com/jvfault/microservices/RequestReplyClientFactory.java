package com.jvfault.microservices;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求-响应客户端代理工厂 - 由接口生成类型安全的 ClientProxy。
 *
 * <p>接口方法默认以方法名为 pattern；@RequestPattern 可自定义。
 * 返回类型约定：T（同步等待响应）或 void（事件发射）。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class RequestReplyClientFactory {

    private final TransportClient client;
    private final long defaultTimeoutMillis;

    public RequestReplyClientFactory(TransportClient client, long defaultTimeoutMillis) {
        this.client = client;
        this.defaultTimeoutMillis = defaultTimeoutMillis;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> api) {
        return (T) Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, new Handler());
    }

    private class Handler implements InvocationHandler {

        private final ConcurrentHashMap<Method, String> patterns = new ConcurrentHashMap<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            String pattern = patterns.computeIfAbsent(method, m -> {
                RequestPattern rp = m.getAnnotation(RequestPattern.class);
                return rp != null ? rp.value() : m.getName();
            });
            Object payload = args != null && args.length > 0 ? args[0] : null;

            if (method.getReturnType() == void.class) {
                client.emit(pattern, payload);
                return null;
            }
            Message response = client.request(pattern, payload, defaultTimeoutMillis);
            MessageCodec codec = new JacksonMessageCodec();
            return codec.decode(response.getData(), method.getReturnType());
        }
    }
}
