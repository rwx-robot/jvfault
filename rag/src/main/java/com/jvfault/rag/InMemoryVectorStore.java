package com.jvfault.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存向量存储（余弦相似度）。
 *
 * @since v0.11.0 (2025)
 * @author jvfault team
 */
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    private static final class Entry {
        final String document;
        final float[] embedding;
        final Map<String, Object> metadata;

        Entry(String document, float[] embedding, Map<String, Object> metadata) {
            this.document = document;
            this.embedding = embedding;
            this.metadata = metadata;
        }
    }

    @Override
    public void add(String id, String document, float[] embedding, Map<String, Object> metadata) {
        entries.put(id, new Entry(document, embedding.clone(), metadata));
    }

    @Override
    public List<SearchHit> similaritySearch(float[] queryEmbedding, int topK) {
        List<SearchHit> hits = new ArrayList<>();
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            double score = cosineSimilarity(queryEmbedding, e.getValue().embedding);
            hits.add(new SearchHit(e.getKey(), e.getValue().document,
                    (float) score, e.getValue().metadata));
        }
        hits.sort(Comparator.comparingDouble(SearchHit::score).reversed());
        return hits.size() > topK ? new ArrayList<>(hits.subList(0, topK)) : hits;
    }

    @Override
    public int size() {
        return entries.size();
    }

    /** 余弦相似度 */
    static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不一致: " + a.length + " vs " + b.length);
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
