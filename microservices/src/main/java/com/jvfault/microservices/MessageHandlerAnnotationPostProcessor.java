package com.jvfault.microservices;

import com.jvfault.core.annotation.Component;
import com.jvfault.core.container.BeanPostProcessor;
import com.jvfault.microservices.annotation.EventPattern;
import com.jvfault.microservices.annotation.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 消息处理器注解后处理器 - 把 Bean 的 @MessageHandler/@EventPattern
 * 方法注册到指定 TransportServer。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
@Component
public class MessageHandlerAnnotationPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(MessageHandlerAnnotationPostProcessor.class);

    private final TransportServer server;
    private final MessageCodec codec;

    public MessageHandlerAnnotationPostProcessor(TransportServer server) {
        this(server, new JacksonMessageCodec());
    }

    public MessageHandlerAnnotationPostProcessor(TransportServer server, MessageCodec codec) {
        this.server = server;
        this.codec = codec;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                MessageHandler mh = method.getAnnotation(MessageHandler.class);
                if (mh != null) {
                    registerHandler(bean, method, mh.value());
                }
                EventPattern ep = method.getAnnotation(EventPattern.class);
                if (ep != null) {
                    registerHandler(bean, method, ep.value());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return bean;
    }

    private void registerHandler(Object bean, Method method, String pattern) {
        method.setAccessible(true);
        Class<?> payloadType = method.getParameterCount() > 0 ? method.getParameterTypes()[0] : byte[].class;
        boolean wantsMessage = method.getParameterCount() > 1 && method.getParameterTypes()[1] == Message.class;
        boolean hasResponse = method.getReturnType() != void.class && method.getReturnType() != Void.class;

        if (!server.isRunning()) {
            server.bind();
        }
        server.subscribe(pattern, request -> {
            try {
                Object payload = codec.decode(request.getData(), payloadType);
                Object result = wantsMessage
                        ? method.invoke(bean, payload, request)
                        : method.invoke(bean, payload);
                if (!hasResponse) {
                    return null;
                }
                String correlationId = request.getCorrelationId() != null
                        ? request.getCorrelationId() : request.getId();
                return new Message(request.getPattern(), codec.encode(result), correlationId, null);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.warn("Handler {} failed: {}", pattern, cause.getMessage());
                return new Message(request.getPattern(), new byte[0], request.getId(),
                        java.util.Collections.singletonMap(Message.HEADER_ERROR, String.valueOf(cause.getMessage())));
            } catch (Exception e) {
                throw new TransportException("handler 调用失败: " + pattern, e);
            }
        });
        log.info("Registered message handler: {} -> {}", pattern, bean.getClass().getSimpleName());
    }
}
