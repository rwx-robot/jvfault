package com.jvfault.cache;

import java.lang.reflect.Method;

/**
 * 缓存键生成器。
 *
 * <p>默认键：类名#方法名(args...)；
 * 支持简单模板：#p0、#p0.fieldName（属性路径反射读取）、#result（仅 CachePut）。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public interface KeyGenerator {

    /**
     * @param template 键模板（空串使用默认键）
     * @param target   目标对象
     * @param method   方法
     * @param args     实参
     * @param result   方法返回值（模板含 #result 时使用，可 null）
     */
    Object generate(String template, Object target, Method method, Object[] args, Object result);

    /** 默认实现 */
    KeyGenerator DEFAULT = new KeyGenerator() {
        @Override
        public Object generate(String template, Object target, Method method, Object[] args, Object result) {
            if (template == null || template.trim().isEmpty()) {
                StringBuilder sb = new StringBuilder(target.getClass().getSimpleName())
                        .append('#').append(method.getName()).append('(');
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        if (i > 0) sb.append(',');
                        sb.append(args[i]);
                    }
                }
                sb.append(')');
                return sb.toString();
            }
            String t = template.trim();
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < t.length()) {
                int hash = t.indexOf("#", i);
                if (hash < 0) {
                    sb.append(t, i, t.length());
                    break;
                }
                sb.append(t, i, hash);
                int end = hash + 1;
                while (end < t.length() && (Character.isLetterOrDigit(t.charAt(end)) || t.charAt(end) == '.')) {
                    end++;
                }
                String expr = t.substring(hash + 1, end);
                if (expr.equals("result")) {
                    sb.append(result);
                } else if (expr.startsWith("p")) {
                    String indexPart = expr.contains(".") ? expr.substring(1, expr.indexOf('.')) : expr.substring(1);
                    int index = Integer.parseInt(indexPart);
                    Object value = args[index];
                    sb.append(expr.contains(".") ? resolvePath(value, expr) : value);
                } else {
                    throw new IllegalArgumentException("无法解析键模板: " + template);
                }
                i = end;
            }
            return sb.toString();
        }

        /** 解析 p0.field.sub 形式的属性路径 */
        private Object resolvePath(Object value, String expr) {
            Object current = value;
            String path = expr.contains(".") ? expr.substring(expr.indexOf('.') + 1) : null;
            if (path == null) {
                return current;
            }
            for (String part : path.split("\\.")) {
                if (current == null) {
                    return null;
                }
                current = readProperty(current, part);
            }
            return current;
        }

        private Object readProperty(Object target, String property) {
            try {
                String getter = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
                try {
                    return target.getClass().getMethod(getter).invoke(target);
                } catch (NoSuchMethodException e) {
                    String is = "is" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
                    return target.getClass().getMethod(is).invoke(target);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("无法读取属性 " + property + " of " + target, e);
            }
        }
    };
}
