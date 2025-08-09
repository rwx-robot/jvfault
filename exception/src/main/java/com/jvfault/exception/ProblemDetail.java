package com.jvfault.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RFC 7807 Problem Detail - 标准化错误响应体。
 *
 * <p>JSON 形如：
 * <pre>{"type":"about:blank","title":"Not Found","status":404,"detail":"User 1"}</pre>
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class ProblemDetail {

    private String type = "about:blank";
    private String title;
    private int status;
    private String detail;
    private String instance;
    private final Map<String, Object> properties = new LinkedHashMap<>();

    public ProblemDetail() {
    }

    public ProblemDetail(int status, String title) {
        this.status = status;
        this.title = title;
    }

    public static ProblemDetail forStatus(int status) {
        return new ProblemDetail(status, reasonPhraseOf(status));
    }

    public static ProblemDetail forStatusAndDetail(int status, String detail) {
        ProblemDetail pd = forStatus(status);
        pd.detail = detail;
        return pd;
    }

    /**
     * 从异常构造 ProblemDetail。
     * HttpException 使用其携带的 ProblemDetail；其余按 500 处理。
     */
    public static ProblemDetail of(Throwable throwable) {
        if (throwable instanceof HttpException) {
            return ((HttpException) throwable).getProblem();
        }
        return forStatusAndDetail(500,
                throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName());
    }

    private static String reasonPhraseOf(int status) {
        switch (status) {
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 409: return "Conflict";
            case 415: return "Unsupported Media Type";
            case 429: return "Too Many Requests";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            default: return "HTTP " + status;
        }
    }

    // ============ JSON 序列化 (手工实现，无 jackson 依赖) ============

    /**
     * 序列化为 RFC 7807 JSON（properties 附加在顶层）。
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{');
        boolean first = true;
        first = writeField(sb, first, "type", type);
        first = writeField(sb, first, "title", title);
        if (status > 0) {
            if (!first) sb.append(',');
            sb.append("\"status\":").append(status);
            first = false;
        }
        first = writeField(sb, first, "detail", detail);
        first = writeField(sb, first, "instance", instance);
        for (Map.Entry<String, Object> e : properties.entrySet()) {
            if (!first) sb.append(',');
            sb.append(JsonWriter.quote(e.getKey())).append(':');
            JsonWriter.write(sb, e.getValue());
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }

    private boolean writeField(StringBuilder sb, boolean first, String name, String value) {
        if (value == null) {
            return first;
        }
        if (!first) {
            sb.append(',');
        }
        sb.append(JsonWriter.quote(name)).append(':').append(JsonWriter.quote(value));
        return false;
    }

    // ============ Getter/Setter ============

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getInstance() { return instance; }
    public void setInstance(String instance) { this.instance = instance; }

    public Map<String, Object> getProperties() { return properties; }

    public ProblemDetail property(String name, Object value) {
        properties.put(name, value);
        return this;
    }
}
