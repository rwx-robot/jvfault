package com.jvfault.ai;

/**
 * 聊天模型 SPI —— 统一各家 LLM 的最小接口。
 * 对应 Spring AI: ChatModel
 *
 * @since v0.11.0 (2025)
 * @author jvfault team
 */
@FunctionalInterface
public interface ChatModel {

    /** 模型标识（如 openai:gpt-4o-mini），默认取实现类简单名 */
    default String getModelId() {
        return getClass().getSimpleName();
    }

    ChatResponse call(ChatRequest request);

    /**
     * 流式调用：逐块回调（阻塞实现亦可，stream 关闭即结束）。
     */
    default void stream(ChatRequest request, java.util.function.Consumer<String> chunkConsumer) {
        chunkConsumer.accept(call(request).content());
    }
}
