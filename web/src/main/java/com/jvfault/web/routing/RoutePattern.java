package com.jvfault.web.routing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路由模式 - 支持 :param 命名段与 * 通配尾段。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class RoutePattern {

    private final String pattern;
    private final Pattern regex;
    private final java.util.List<String> paramNames;
    private int literalSegments;

    public RoutePattern(String pattern) {
        this.pattern = normalize(pattern);
        this.paramNames = new java.util.ArrayList<>();
        this.regex = compile(this.pattern);
    }

    private static String normalize(String pattern) {
        String p = pattern.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        while (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private Pattern compile(String normalized) {
        String[] segments = normalized.split("/");
        StringBuilder regex = new StringBuilder("^");
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            regex.append('/');
            if (segment.equals("*")) {
                regex.append("(.*)"); // 通配尾段
                paramNames.add("*");
            } else if (segment.startsWith(":")) {
                paramNames.add(segment.substring(1));
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(segment));
                literalSegments++;
            }
        }
        if (normalized.equals("/")) {
            regex.append('/');
        }
        regex.append("/?$");
        return Pattern.compile(regex.toString());
    }

    /**
     * 匹配路径并提取路径参数；不匹配返回 null。
     */
    public Map<String, String> match(String path) {
        Matcher matcher = regex.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < paramNames.size(); i++) {
            String value = matcher.group(i + 1);
            if (value != null) {
                params.put(paramNames.get(i), value);
            }
        }
        return params;
    }

    public String getPattern() {
        return pattern;
    }

    /** 字面段数量（路由优先级：字面越多越优先） */
    public int getSpecificity() {
        return literalSegments;
    }

    @Override
    public String toString() {
        return pattern;
    }
}
