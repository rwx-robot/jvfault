package com.jvfault.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 聊天请求。
 *
 * @since v0.11.0 (2025)
 * @author jvfault team
 */
public class ChatRequest {

    private final List<Message> messages = new ArrayList<>();
    private final Map<String, Object> options = new LinkedHashMap<>();
    private final List<Tool> tools = new ArrayList<>();

    public ChatRequest system(String content) {
        messages.add(new Message("system", content));
        return this;
    }

    public ChatRequest user(String content) {
        messages.add(new Message("user", content));
        return this;
    }

    public ChatRequest assistant(String content) {
        messages.add(new Message("assistant", content));
        return this;
    }

    public ChatRequest option(String key, Object value) {
        options.put(key, value);
        return this;
    }

    public ChatRequest tool(Tool tool) {
        tools.add(tool);
        return this;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public List<Tool> getTools() {
        return tools;
    }

    /** 消息 */
    public record Message(String role, String content) {
    }

    /** 工具（函数调用） */
    public record Tool(String name, String description, String jsonSchema) {
    }
}
