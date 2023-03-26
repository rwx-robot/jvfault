package com.jvfault.security.jwt;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWT (HS256) 签发与校验 —— 基于 JDK JCE，无第三方依赖。
 *
 * @since v1.0.0 (2026)
 * @author jvfault team
 */
public class JwtService {

    private final byte[] secret;
    private final long defaultTtlSeconds;
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64DEC = Base64.getUrlDecoder();

    public JwtService(String secret, long defaultTtlSeconds) {
        if (secret == null || secret.length() < 8) {
            throw new IllegalArgumentException("JWT secret 至少 8 字符");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    /**
     * 签发 JWT（HS256）。
     *
     * @param claims 载荷声明（iss/sub/exp 等标准声明可自定义）
     */
    public String issue(Map<String, Object> claims) {
        return issue(claims, defaultTtlSeconds);
    }

    public String issue(Map<String, Object> claims, long ttlSeconds) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> payload = new LinkedHashMap<>(claims);
        payload.put("iat", now);
        payload.put("exp", now + ttlSeconds);

        String encodedHeader = B64URL.encodeToString(json(header));
        String encodedPayload = B64URL.encodeToString(json(payload));
        String signingInput = encodedHeader + "." + encodedPayload;
        return signingInput + "." + B64URL.encodeToString(sign(signingInput));
    }

    /**
     * 校验并解析；无效/过期返回 null。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verify(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = sign(signingInput);
        byte[] provided;
        try {
            provided = B64DEC.decode(parts[2]);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!MessageDigest.isEqual(expected, provided)) {
            return null;
        }
        Map<String, Object> payload;
        try {
            payload = new Jacksonish().parse(new String(B64DEC.decode(parts[1]), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
        Object exp = payload.get("exp");
        if (exp instanceof Number && ((Number) exp).longValue() < System.currentTimeMillis() / 1000) {
            return null;
        }
        return payload;
    }

    private byte[] sign(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 签名失败", e);
        }
    }

    private byte[] json(Map<String, Object> map) {
        return new Jacksonish().serialize(map).getBytes(StandardCharsets.UTF_8);
    }

    /** 极简 JSON 序列化/解析（仅支持本模块的声明场景） */
    static final class Jacksonish {
        String serialize(Map<String, Object> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(e.getKey())).append(':').append(value(e.getValue()));
            }
            return sb.append('}').toString();
        }

        private String value(Object v) {
            if (v == null) return "null";
            if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
            return quote(String.valueOf(v));
        }

        private String quote(String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }

        Map<String, Object> parse(String json) {
            Map<String, Object> out = new LinkedHashMap<>();
            int i = 1; // skip {
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '}' || c == ',') { i++; continue; }
                if (c != '"') { i++; continue; }
                int[] keyEnd = stringEnd(json, i);
                String key = json.substring(keyEnd[0] + 1, keyEnd[1]);
                i = keyEnd[1] + 1;
                while (i < json.length() && json.charAt(i) != ':') i++;
                i++;
                while (i < json.length() && (json.charAt(i) == ' ')) i++;
                if (i >= json.length()) break;
                if (json.charAt(i) == '"') {
                    int[] vEnd = stringEnd(json, i);
                    out.put(key, json.substring(vEnd[0] + 1, vEnd[1]));
                    i = vEnd[1] + 1;
                } else {
                    int end = i;
                    while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) end++;
                    String raw = json.substring(i, end).trim();
                    out.put(key, raw.equals("null") ? null
                            : raw.equals("true") ? Boolean.TRUE
                            : raw.equals("false") ? Boolean.FALSE
                            : (Object) Double.parseDouble(raw));
                    i = end;
                }
            }
            return out;
        }

        /** 返回 [start, endExclusiveOfClosingQuote]，处理转义 */
        private int[] stringEnd(String s, int start) {
            int i = start + 1;
            while (i < s.length()) {
                if (s.charAt(i) == '\\') { i += 2; continue; }
                if (s.charAt(i) == '"') {
                    return new int[]{start, i};
                }
                i++;
            }
            throw new IllegalArgumentException("非法 JSON 字符串");
        }
    }
}
