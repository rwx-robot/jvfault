package com.jvfault.example.v110;

import com.jvfault.ai.ChatModel;
import com.jvfault.ai.ChatRequest;
import com.jvfault.ai.ChatResponse;
import com.jvfault.rag.InMemoryVectorStore;
import com.jvfault.rag.RagPipeline;

import java.util.Map;

public class Application {

    public static void main(String[] args) {
        System.out.println("== jvfault v0.11.0: AI / RAG ==");

        // 知识库（真实场景: EmbeddingModel + PGVector/Milvus）
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.add("k1", "jvfault 是一个注解驱动的模块化 Java 框架", embed("jvfault modular java framework"), Map.of());
        store.add("k2", "jvfault 支持模块化与依赖注入", embed("jvfault modules dependency injection"), Map.of());
        store.add("k3", "今天天气不错", embed("nice weather today"), Map.of());

        RagPipeline pipeline = new RagPipeline(store, Application::embed, 2);
        String answer = pipeline.generate("jvfault 是什么框架？", prompt -> {
            System.out.println("  增强后的 prompt 包含知识库片段: " + prompt.contains("注解驱动"));
            return "（LLM 生成）jvfault 是一个注解驱动的模块化 Java 框架。";
        });
        System.out.println("  答案: " + answer);

        // ChatModel SPI（stub；生产用 OpenAiCompatibleChatModel 指向真实端点）
        ChatModel model = request -> new ChatResponse("ok", "stub", new ChatResponse.Usage(1, 1, 2));
        ChatResponse response = model.call(new ChatRequest().system("sys").user("hi"));
        System.out.println("  ChatModel: " + response.model() + " -> " + response.content());
        System.out.println("== 完成 ==");
    }

    static float[] embed(String text) {
        float[] v = new float[8];
        for (int i = 0; i < text.length(); i++) {
            v[i % 8] += (text.charAt(i) % 32) / 32.0f;
        }
        return v;
    }
}
