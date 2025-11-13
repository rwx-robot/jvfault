package com.jvfault.microservices;

/**
 * 模式匹配器 - 现代框架 风格通配。
 *
 * <p>语义：精确匹配、{@code *} 单段、{@code #} 跨段。
 * 例：{@code user.*.created} 匹配 user/42/created；
 * {@code log.#} 匹配 log/a/b/c。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public final class PatternMatcher {

    private PatternMatcher() {
    }

    /**
     * @param pattern 订阅模式
     * @param topic   实际消息 pattern
     */
    public static boolean matches(String pattern, String topic) {
        if (pattern == null || topic == null) {
            return false;
        }
        if (pattern.equals(topic)) {
            return true;
        }
        String[] p = pattern.split("\\.");
        String[] t = topic.split("\\.");
        return matchSegments(p, 0, t, 0);
    }

    private static boolean matchSegments(String[] p, int pi, String[] t, int ti) {
        if (pi == p.length) {
            return ti == t.length;
        }
        String seg = p[pi];
        if ("#".equals(seg)) {
            // # 匹配零或多段
            for (int k = ti; k <= t.length; k++) {
                if (matchSegments(p, pi + 1, t, k)) {
                    return true;
                }
            }
            return false;
        }
        if (ti >= t.length) {
            return false;
        }
        if ("*".equals(seg) || seg.equals(t[ti])) {
            return matchSegments(p, pi + 1, t, ti + 1);
        }
        return false;
    }
}
