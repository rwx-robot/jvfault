package com.jvfault.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvfault.web.annotation.*;
import com.jvfault.web.http.HttpContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 处理器调用器 - 参数绑定与反射调用。
 *
 * <p>绑定规则：
 * <ul>
 *   <li>@Param / @Query / @Header：按名取字符串转目标类型</li>
 *   <li>@Body：JSON 反序列化</li>
 *   <li>HttpContext 类型直接注入</li>
 * </ul>
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class HandlerInvoker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Object invoke(HttpContext context, Object controller, Method handler) throws Exception {
        Parameter[] parameters = handler.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolveArgument(context, parameters[i]);
        }
        handler.setAccessible(true);
        try {
            return handler.invoke(controller, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof Exception) {
                throw (Exception) cause; // 保留业务异常类型（供全局异常处理识别）
            }
            throw new IllegalStateException(cause);
        }
    }

    private Object resolveArgument(HttpContext context, Parameter parameter) {
        Class<?> type = parameter.getType();
        if (type == HttpContext.class) {
            return context;
        }

        Param param = parameter.getAnnotation(Param.class);
        if (param != null) {
            String name = !param.value().isEmpty() ? param.value() : parameter.getName();
            return convert(context.getParam(name), type, "path:" + name);
        }

        Query query = parameter.getAnnotation(Query.class);
        if (query != null) {
            String name = !query.value().isEmpty() ? query.value() : parameter.getName();
            return convert(context.getQuery(name), type, "query:" + name);
        }

        Header header = parameter.getAnnotation(Header.class);
        if (header != null) {
            String name = !header.value().isEmpty() ? header.value() : parameter.getName();
            return convert(context.getRequest().getHeader(name), type, "header:" + name);
        }

        if (parameter.isAnnotationPresent(Body.class)) {
            byte[] raw = context.getRequest().getBody();
            if (raw == null || raw.length == 0) {
                throw new com.jvfault.exception.BadRequestException("请求体为空");
            }
            try {
                return MAPPER.readValue(raw, MAPPER.getTypeFactory().constructType(parameter.getParameterizedType()));
            } catch (Exception e) {
                throw new com.jvfault.exception.BadRequestException(
                        "请求体反序列化失败: " + type.getSimpleName());
            }
        }

        throw new IllegalArgumentException("无法绑定的处理器参数（缺少注解）: "
                + parameter.getDeclaringExecutable().getName() + "#" + parameter.getName());
    }

    private Object convert(String raw, Class<?> type, String source) {
        if (raw == null) {
            return null;
        }
        if (type == String.class) {
            return raw;
        }
        try {
            if (type == int.class || type == Integer.class) return Integer.valueOf(raw);
            if (type == long.class || type == Long.class) return Long.valueOf(raw);
            if (type == boolean.class || type == Boolean.class) return Boolean.valueOf(raw);
            if (type == double.class || type == Double.class) return Double.valueOf(raw);
            if (type == float.class || type == Float.class) return Float.valueOf(raw);
            if (type.isEnum()) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object e = Enum.valueOf((Class<? extends Enum>) type, raw);
                return e;
            }
        } catch (Exception e) {
            throw new com.jvfault.exception.BadRequestException("参数类型转换失败: " + source + "=" + raw);
        }
        throw new com.jvfault.exception.BadRequestException("不支持的参数类型: " + type.getName());
    }
}
