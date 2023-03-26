package com.jvfault.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-rag 核心测试
 *
 * @since v0.11.0 (2025)
 */
@DisplayName("RAG 模块测试")
class RagModuleTest {

    /** 简单哈希嵌入（确定性，供测试） */
    static float[] embed(String text) {
        float[] v = new float[8];
        for (int i = 0; i < text.length(); i++) {
            v[i % 8] += (text.charAt(i) % 32) / 32.0f;
        }
        return v;
    }

    @Test
    @DisplayName("余弦相似度计算")
    void testCosine() {
        assertEquals(1.0, InMemoryVectorStore.cosineSimilarity(
                new float[]{1, 0, 0}, new float[]{1, 0, 0}), 1e-6);
        assertEquals(0.0, InMemoryVectorStore.cosineSimilarity(
                new float[]{1, 0}, new float[]{0, 1}), 1e-6);
        assertThrows(IllegalArgumentException.class,
                () -> InMemoryVectorStore.cosineSimilarity(new float[]{1}, new float[]{1, 2}));
    }

    @Test
    @DisplayName("向量存储相似度检索与 topK")
    void testVectorStore() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.add("1", "jvfault 是一个 Java 框架", embed("jvfault java framework"), Map.of("src", "doc1"));
        store.add("2", "今天的天气很好", embed("sunny weather today"), Map.of());
        store.add("3", "Spring 是另一个 Java 生态", embed("spring java ecosystem"), Map.of());

        assertEquals(3, store.size());

        List<VectorStore.SearchHit> hits = store.similaritySearch(embed("java framework ecosystem"), 2);
        assertEquals(2, hits.size());
        assertTrue(hits.get(0).score() >= hits.get(1).score(), "按分数降序");
        assertTrue(hits.get(0).document().contains("Java") || hits.get(0).document().contains("Spring"));
    }

    @Test
    @DisplayName("TextChunker 滑动窗口与句子边界")
    void testChunker() {
        TextChunker chunker = new TextChunker(50, 10);
        String doc = "这是第一句话。这是第二句话。这是第三句话。这是第四句话。";
        List<String> chunks = chunker.chunk(doc);
        assertFalse(chunks.isEmpty());
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 50, "块长不超过 50: " + chunk.length());
        }
        // 重叠语义: 相邻块共享内容
        assertTrue(chunker.chunk("").isEmpty());

        assertThrows(IllegalArgumentException.class, () -> new TextChunker(10, 10));
    }

    @Test
    @DisplayName("RagPipeline 检索增强生成端到端")
    void testPipeline() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.add("d1", "jvfault 支持模块化架构", embed("jvfault modular architecture"), Map.of());
        store.add("d2", "无关内容", embed("unrelated stuff"), Map.of());

        RagPipeline pipeline = new RagPipeline(store, RagModuleTest::embed, 1);
        assertEquals(1, pipeline.getTopK());

        String context = pipeline.buildContext("jvfault architecture");
        assertTrue(context.contains("模块化"), "上下文应包含命中片段: " + context);

        String answer = pipeline.generate("jvfault architecture", prompt -> {
            assertTrue(prompt.contains("资料"), "prompt 应注入上下文");
            return "based-on-context";
        });
        assertEquals("based-on-context", answer);
    }
}
