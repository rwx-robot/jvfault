package com.jvfault.rag;

import java.util.List;
import java.util.Map;

/**
 * 向量存储 SPI。
 * 对应 Spring AI: VectorStore
 *
 * @since v0.11.0 (2025)
 * @author jvfault team
 */
public interface VectorStore {

    void add(String id, String document, float[] embedding, Map<String, Object> metadata);

    /**
     * 相似度检索（余弦相似度降序，topK）。
     */
    List<SearchHit> similaritySearch(float[] queryEmbedding, int topK);

    int size();

    /** 检索命中 */
    record SearchHit(String id, String document, float score, Map<String, Object> metadata) {
    }
}
