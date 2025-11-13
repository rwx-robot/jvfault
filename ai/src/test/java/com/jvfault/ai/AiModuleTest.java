package com.jvfault.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-ai 核心测试（不依赖真实 LLM 服务）
 *
 * @since v0.11.0 (2025)
 */
@DisplayName("AI 模块测试")
class AiModuleTest {

    @Test
    @DisplayName("ChatRequest 构建器（角色/选项/工具）")
    void testRequestBuilder() {
        ChatRequest request = new ChatRequest()
                .system("你是一个助手")
                .user("hello")
                .option("temperature", 0.7)
                .tool(new ChatRequest.Tool("get_weather", "查询天气", "{\"type\":\"object\"}"));

        assertEquals(2, request.getMessages().size());
        assertEquals("system", request.getMessages().get(0).role());
        assertEquals("user", request.getMessages().get(1).role());
        assertEquals(0.7, request.getOptions().get("temperature"));
        assertEquals(1, request.getTools().size());
        assertEquals("get_weather", request.getTools().get(0).name());
    }

    @Test
    @DisplayName("ChatResponse record 语义")
    void testResponseRecord() {
        ChatResponse response = new ChatResponse("hi", "test-model",
                new ChatResponse.Usage(10, 5, 15));
        assertEquals("hi", response.content());
        assertEquals(15, response.usage().totalTokens());
    }

    @Test
    @DisplayName("默认 stream 退化为单块回调")
    void testDefaultStreamFallback() {
        ChatModel model = request -> new ChatResponse("full-answer", "stub",
                new ChatResponse.Usage(0, 0, 0));

        StringBuilder received = new StringBuilder();
        model.stream(new ChatRequest().user("x"), received::append);
        assertEquals("full-answer", received.toString());
    }

    @Test
    @DisplayName("OpenAI 兼容模型：请求体构建（反射验证非空路径）")
    void testOpenAiCompatibleBuild() throws Exception {
        OpenAiCompatibleChatModel model = new OpenAiCompatibleChatModel(
                "http://127.0.0.1:1", "sk-test", "gpt-4o-mini");
        assertEquals("openai:gpt-4o-mini", model.getModelId());

        // 调用不可达端点 -> IllegalStateException（构造的请求体被服务端拒绝前先网络失败）
        assertThrows(IllegalStateException.class,
                () -> model.call(new ChatRequest().user("hi")));
    }

    @Test
    @DisplayName("内存脚本模型用于测试注入")
    void testStubModel() {
        ChatModel model = request -> new ChatResponse("echo:" +
                request.getMessages().get(request.getMessages().size() - 1).content(),
                "stub", new ChatResponse.Usage(1, 1, 2));

        ChatResponse response = model.call(new ChatRequest().user("jvfault"));
        assertEquals("echo:jvfault", response.content());
    }
}
