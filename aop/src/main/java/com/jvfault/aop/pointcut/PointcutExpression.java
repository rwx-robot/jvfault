package com.jvfault.aop.pointcut;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Pointcut 表达式解析与匹配（execution 子集）。
 *
 * <p>支持的表达式形式：
 * <pre>
 *   execution(* com.example.service.*Service.save*(..))   // 通配段与任意参数
 *   execution(public String com.example..*Controller.handle(..))
 *   execution(int com.example.Calc.add())                 // 无参
 * </pre>
 *
 * <p>子集范围：
 * <ul>
 *   <li>修饰符可省略；省略时不校验修饰符</li>
 *   <li>类名模式支持 {@code *}（单段）与 {@code ..}（跨段）；方法名支持 {@code *} 前后缀</li>
 *   <li>返回类型支持 {@code *} 或类型名（简单名或全限定名）</li>
 *   <li>参数表支持 {@code (..)}（任意）、{@code ()}（无参）、逗号分隔类型（按简单名匹配，末尾 {@code ..} 吞剩余）</li>
 * </ul>
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public final class PointcutExpression {

    private final String raw;
    private final String modifier;   // null 表示不校验
    private final String returnType;
    private final List<String> classSegments; // 类名段，".. " 表示跨段
    private final String methodName;
    private final String argsPattern;         // "..", "", 或类型模式串

    private PointcutExpression(String raw, String modifier, String returnType,
                               List<String> classSegments, String methodName, String argsPattern) {
        this.raw = raw;
        this.modifier = modifier;
        this.returnType = returnType;
        this.classSegments = classSegments;
        this.methodName = methodName;
        this.argsPattern = argsPattern;
    }

    /**
     * 解析表达式，非法格式抛 IllegalArgumentException。
     */
    public static PointcutExpression parse(String expression) {
        String raw = expression == null ? "" : expression.trim();
        if (!raw.startsWith("execution(") || !raw.endsWith(")")) {
            throw new IllegalArgumentException("仅支持 execution(..) 表达式: " + expression);
        }
        String body = raw.substring("execution(".length(), raw.length() - 1).trim();
        int paren = body.lastIndexOf('(');
        if (paren < 0 || !body.endsWith(")")) {
            throw new IllegalArgumentException("缺少方法参数段: " + expression);
        }
        String argsPattern = body.substring(paren + 1, body.length() - 1).trim();
        String signature = body.substring(0, paren).trim();

        // 方法名与类名分离：惰性前缀 + 显式捕获 1~2 个点的分隔符（2 点即跨段 ".."）
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("^(.*?)(\\.{1,2})([\\w$*]+)$").matcher(signature);
        if (!m.matches()) {
            throw new IllegalArgumentException("无法解析类名/方法名: " + expression);
        }
        String methodName = m.group(3);
        String classFqn = m.group(1).trim() + (m.group(2).length() == 2 ? ".." : "");
        if (classFqn.isEmpty() || methodName.isEmpty()) {
            throw new IllegalArgumentException("类名或方法名为空: " + expression);
        }

        // 返回类型与修饰符
        String[] headParts = classFqn.trim().split("\\s+");
        String returnType;
        String modifier = null;
        String classPattern;
        if (headParts.length == 1) {
            returnType = "*";
            classPattern = headParts[0];
        } else if (headParts.length == 2) {
            returnType = headParts[0];
            classPattern = headParts[1];
        } else if (headParts.length == 3) {
            modifier = headParts[0];
            returnType = headParts[1];
            classPattern = headParts[2];
        } else {
            throw new IllegalArgumentException("签名段过多: " + expression);
        }

        List<String> segments = new ArrayList<>();
        // ".." 先替换为占位段，避免 split 丢失跨段语义（"bootstrap..*" → bootstrap/../*）
        String normalized = classPattern.replace("..", ".\u0002.");
        for (String seg : normalized.split("\\.", -1)) {
            // 注意：trim() 会剥掉 <= U+0020 的字符（含占位符 U+0002），必须先判占位符
            if ("\u0002".equals(seg)) {
                segments.add("..");
                continue;
            }
            String s = seg.trim();
            if (s.isEmpty()) {
                continue; // 首尾点号
            }
            segments.add(s);
        }

        return new PointcutExpression(raw, modifier, returnType, segments, methodName, argsPattern);
    }

    /**
     * 判断目标方法是否匹配本表达式。
     *
     * @param targetClass Bean 实现类
     * @param method 目标方法
     */
    public boolean matches(Class<?> targetClass, Method method) {
        if (!matchSegments(classSegments, targetClass.getName().split("\\."))) {
            return false;
        }
        if (!matchSimpleName(methodName, method.getName())) {
            return false;
        }
        if (!"*".equals(returnType)) {
            String ret = method.getReturnType().getName();
            if (!ret.equals(returnType)
                    && !ret.substring(ret.lastIndexOf('.') + 1).equals(returnType)
                    && !method.getReturnType().getSimpleName().equals(returnType)) {
                return false;
            }
        }
        if (modifier != null && !modifier.equals(java.lang.reflect.Modifier.toString(method.getModifiers()))) {
            return false;
        }
        return matchArgs(argsPattern, method.getParameterTypes());
    }

    private static boolean matchSegments(List<String> patternSegs, String[] classSegs) {
        int p = 0, c = 0;
        boolean starStar = false;
        while (p < patternSegs.size()) {
            String seg = patternSegs.get(p);
            if ("..".equals(seg)) {
                // ".." 匹配零或多段；先跳过连续 ".."
                if (p == patternSegs.size() - 1) {
                    return true; // 末尾 .. 匹配剩余所有段
                }
                starStar = true;
                // 尝试让下一个模式段匹配 classSegs 中任意后续位置
                String next = patternSegs.get(p + 1);
                int k = c;
                while (k < classSegs.length) {
                    if (matchesSegment(next, classSegs[k])
                            && matchSegments(patternSegs.subList(p + 1, patternSegs.size()), subarray(classSegs, k))) {
                        return true;
                    }
                    k++;
                }
                return false;
            }
            if (c >= classSegs.length) {
                return false;
            }
            if (!matchesSegment(seg, classSegs[c])) {
                return false;
            }
            c++;
            p++;
            starStar = false;
        }
        boolean consumed = c == classSegs.length;
        return consumed || starStar;
    }

    private static String[] subarray(String[] arr, int from) {
        String[] out = new String[arr.length - from];
        System.arraycopy(arr, from, out, 0, out.length);
        return out;
    }

    private static boolean matchesSegment(String pattern, String segment) {
        // 段内通配与类名模式一致：*、前缀*、*后缀、精确
        return matchSimpleName(pattern, segment);
    }

    /** 方法名模式：*、前缀*、*后缀、精确 */
    private static boolean matchSimpleName(String pattern, String name) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (pattern.endsWith("*") && pattern.startsWith("*")) {
            return name.contains(pattern.substring(1, pattern.length() - 1));
        }
        if (pattern.endsWith("*")) {
            return name.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) {
            return name.endsWith(pattern.substring(1));
        }
        return pattern.equals(name);
    }

    private static boolean matchArgs(String argsPattern, Class<?>[] paramTypes) {
        if ("..".equals(argsPattern)) {
            return true;
        }
        if (argsPattern.isEmpty()) {
            return paramTypes.length == 0;
        }
        String[] parts = argsPattern.split(",");
        int i = 0;
        for (; i < parts.length; i++) {
            String part = parts[i].trim();
            if ("..".equals(part)) {
                return true; // 末尾 .. 吞剩余
            }
            if (i >= paramTypes.length) {
                return false;
            }
            if (!"*".equals(part) && !matchesType(part, paramTypes[i])) {
                return false;
            }
        }
        return i == paramTypes.length;
    }

    private static boolean matchesType(String pattern, Class<?> type) {
        String simple = type.getSimpleName();
        String fqn = type.getName();
        return pattern.equals(simple) || pattern.equals(fqn);
    }

    public String getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return raw;
    }
}
