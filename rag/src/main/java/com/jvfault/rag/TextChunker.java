package com.jvfault.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分块器（滑动窗口 + 重叠）。
 *
 * @since v0.11.0 (2025)
 * @author jvfault team
 */
public class TextChunker {

    private final int chunkSize;
    private final int overlap;

    public TextChunker(int chunkSize, int overlap) {
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("重叠必须小于块大小");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    /**
     * 按句子边界优先、字符长度兜底分块。
     */
    public List<String> chunk(String document) {
        List<String> chunks = new ArrayList<>();
        if (document == null || document.isEmpty()) {
            return chunks;
        }
        int start = 0;
        while (start < document.length()) {
            int end = Math.min(start + chunkSize, document.length());
            if (end < document.length()) {
                // 在窗口内向后找句子边界
                int period = document.lastIndexOf('。', end);
                int dot = document.lastIndexOf(". ", end);
                int boundary = Math.max(period, dot);
                if (boundary > start + chunkSize / 2) {
                    end = boundary + 1;
                }
            }
            chunks.add(document.substring(start, end).trim());
            if (end >= document.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }
}
