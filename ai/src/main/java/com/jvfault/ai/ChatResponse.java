package com.jvfault.ai;

/**
 * 聊天响应。
 *
 * @param content 模型输出
 * @param model   模型标识
 * @param usage   token 用量（promptTokens/completionTokens/totalTokens）
 * @since v0.11.0 (2025)
 * @author jvfault team
 */
public record ChatResponse(String content, String model, Usage usage) {

    public record Usage(long promptTokens, long completionTokens, long totalTokens) {
    }
}
